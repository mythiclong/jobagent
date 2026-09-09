package com.zhihang.jobagent.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public final class H2DatabaseRecoverySupport {

    static final String DATASOURCE_URL_KEY = "spring.datasource.url";
    static final String DATASOURCE_USERNAME_KEY = "spring.datasource.username";
    static final String DATASOURCE_PASSWORD_KEY = "spring.datasource.password";
    static final String RECOVERY_ATTEMPTED_FLAG = "jobagent.h2.recovery.attempted";

    private static final String FILE_URL_PREFIX = "jdbc:h2:file:";
    private static final DateTimeFormatter BACKUP_SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private H2DatabaseRecoverySupport() {
    }

    public static boolean recoverIfCorrupted(Throwable startupFailure, String[] args) {
        if (!isCorruptedH2Failure(startupFailure)) {
            return false;
        }
        if (Boolean.getBoolean(RECOVERY_ATTEMPTED_FLAG)) {
            return false;
        }

        Optional<H2DatabaseSettings> settings = resolveSettings(args);
        if (settings.isEmpty()) {
            return false;
        }

        System.setProperty(RECOVERY_ATTEMPTED_FLAG, "true");
        H2DatabaseSettings databaseSettings = settings.get();

        try {
            recoverDatabase(databaseSettings);
            return true;
        } catch (Exception recoveryFailure) {
            startupFailure.addSuppressed(recoveryFailure);
            System.err.println("[jobagent] H2 自动恢复失败，保留原始损坏库并停止启动。");
            recoveryFailure.printStackTrace(System.err);
            return false;
        }
    }

    static boolean isCorruptedH2Failure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (StringUtils.hasText(message)) {
                String normalizedMessage = message.toLowerCase(Locale.ROOT);
                if (normalizedMessage.contains("file corrupted while reading record")
                        || normalizedMessage.contains("[90030-")
                        || normalizedMessage.contains("mvstoreexception")
                        || normalizedMessage.contains("double mark:")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    static Optional<H2DatabaseSettings> resolveSettings(String[] args) {
        String datasourceUrl = firstNonBlank(
                readCommandLineArgument(args, DATASOURCE_URL_KEY),
                System.getProperty(DATASOURCE_URL_KEY),
                System.getenv("SPRING_DATASOURCE_URL"),
                readApplicationProperty(DATASOURCE_URL_KEY)
        );
        if (!StringUtils.hasText(datasourceUrl) || !datasourceUrl.startsWith(FILE_URL_PREFIX)) {
            return Optional.empty();
        }

        Optional<H2FileDatabaseSpec> fileSpec = parseFileDatabaseSpec(datasourceUrl);
        if (fileSpec.isEmpty()) {
            return Optional.empty();
        }

        String username = firstNonBlank(
                readCommandLineArgument(args, DATASOURCE_USERNAME_KEY),
                System.getProperty(DATASOURCE_USERNAME_KEY),
                System.getenv("SPRING_DATASOURCE_USERNAME"),
                readApplicationProperty(DATASOURCE_USERNAME_KEY),
                "sa"
        );
        String password = Objects.toString(firstNonBlank(
                readCommandLineArgument(args, DATASOURCE_PASSWORD_KEY),
                System.getProperty(DATASOURCE_PASSWORD_KEY),
                System.getenv("SPRING_DATASOURCE_PASSWORD"),
                readApplicationProperty(DATASOURCE_PASSWORD_KEY)
        ), "");

        return Optional.of(new H2DatabaseSettings(fileSpec.get(), username, password));
    }

    static Optional<H2FileDatabaseSpec> parseFileDatabaseSpec(String datasourceUrl) {
        if (!StringUtils.hasText(datasourceUrl) || !datasourceUrl.startsWith(FILE_URL_PREFIX)) {
            return Optional.empty();
        }

        String rawPath = datasourceUrl.substring(FILE_URL_PREFIX.length());
        int parameterIndex = rawPath.indexOf(';');
        if (parameterIndex >= 0) {
            rawPath = rawPath.substring(0, parameterIndex);
        }
        if (!StringUtils.hasText(rawPath)) {
            return Optional.empty();
        }

        if (rawPath.startsWith("~/")) {
            rawPath = System.getProperty("user.home") + rawPath.substring(1);
        }

        Path basePath = Paths.get(rawPath);
        if (!basePath.isAbsolute()) {
            basePath = Paths.get(System.getProperty("user.dir")).resolve(basePath).normalize();
        } else {
            basePath = basePath.normalize();
        }

        Path directory = basePath.getParent();
        if (directory == null) {
            directory = Paths.get(System.getProperty("user.dir")).normalize();
        }

        String databaseName = basePath.getFileName().toString();
        return Optional.of(new H2FileDatabaseSpec(datasourceUrl, directory, databaseName, basePath));
    }

    static void recoverDatabase(H2DatabaseSettings settings) throws IOException, SQLException {
        H2FileDatabaseSpec fileSpec = settings.fileSpec();
        Files.createDirectories(fileSpec.directory());

        Path sqlScript = fileSpec.directory().resolve(fileSpec.databaseName() + ".h2.sql");
        Path pageDump = fileSpec.directory().resolve(fileSpec.databaseName() + ".mv.txt");
        backupIfExists(sqlScript, "stale-recovery");
        backupIfExists(pageDump, "stale-recovery");

        System.err.println("[jobagent] 检测到 H2 文件库损坏，开始自动恢复: " + fileSpec.mvStoreFile());
        invokeRecover(fileSpec.directory().toString(), fileSpec.databaseName());

        if (!Files.exists(sqlScript)) {
            throw new IOException("H2 Recover 未生成恢复脚本: " + sqlScript);
        }

        backupIfExists(fileSpec.mvStoreFile(), "corrupted");
        backupIfExists(fileSpec.traceFile(), "corrupted");

        invokeRunScript(settings, sqlScript);

        System.err.println("[jobagent] H2 自动恢复完成，已根据恢复脚本重建数据库。");
    }

    private static void invokeRecover(String directory, String databaseName) throws SQLException {
        try {
            Class<?> recoverClass = Class.forName("org.h2.tools.Recover");
            Method executeMethod = recoverClass.getMethod("execute", String.class, String.class);
            executeMethod.invoke(null, directory, databaseName);
        } catch (InvocationTargetException exception) {
            Throwable targetException = exception.getTargetException();
            if (targetException instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("调用 H2 Recover 失败", targetException);
        } catch (ReflectiveOperationException exception) {
            throw new SQLException("当前运行环境未包含 org.h2.tools.Recover", exception);
        }
    }

    private static void invokeRunScript(H2DatabaseSettings settings, Path sqlScript) throws SQLException {
        try {
            Class<?> runScriptClass = Class.forName("org.h2.tools.RunScript");
            Method executeMethod = runScriptClass.getMethod(
                    "execute",
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    java.nio.charset.Charset.class,
                    boolean.class
            );
            executeMethod.invoke(
                    null,
                    settings.fileSpec().restoreUrl(),
                    settings.username(),
                    settings.password(),
                    sqlScript.toString(),
                    StandardCharsets.UTF_8,
                    true
            );
        } catch (InvocationTargetException exception) {
            Throwable targetException = exception.getTargetException();
            if (targetException instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("调用 H2 RunScript 失败", targetException);
        } catch (ReflectiveOperationException exception) {
            throw new SQLException("当前运行环境未包含 org.h2.tools.RunScript", exception);
        }
    }

    private static void backupIfExists(Path source, String label) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        String timestamp = LocalDateTime.now().format(BACKUP_SUFFIX_FORMATTER);
        String targetFileName = source.getFileName() + "." + label + "-" + timestamp + ".bak";
        Path target = source.resolveSibling(targetFileName);
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        System.err.println("[jobagent] 已备份文件: " + source + " -> " + target);
    }

    private static String readCommandLineArgument(String[] args, String key) {
        if (args == null) {
            return "";
        }
        String prefix = "--" + key + "=";
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(argument -> argument.startsWith(prefix))
                .map(argument -> argument.substring(prefix.length()))
                .findFirst()
                .orElse("");
    }

    private static String readApplicationProperty(String key) {
        Properties properties = new Properties();
        try (InputStream inputStream = new ClassPathResource("application.properties").getInputStream()) {
            properties.load(inputStream);
            return properties.getProperty(key, "");
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        return Arrays.stream(values)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElse("");
    }

    record H2DatabaseSettings(H2FileDatabaseSpec fileSpec, String username, String password) {
    }

    record H2FileDatabaseSpec(String datasourceUrl, Path directory, String databaseName, Path basePath) {

        Path mvStoreFile() {
            return directory.resolve(databaseName + ".mv.db");
        }

        Path traceFile() {
            return directory.resolve(databaseName + ".trace.db");
        }

        String restoreUrl() {
            return FILE_URL_PREFIX + basePath.toString().replace('\\', '/');
        }
    }
}
