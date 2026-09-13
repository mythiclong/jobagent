# 简历分析 Prompt

<!-- prompt-id: resume-analysis -->
<!-- prompt-version: v1 -->

请分析下面这份简历与目标岗位的匹配情况，并严格返回一个 JSON 对象。

输入信息：

- 目标岗位：`{jobName}`
- 岗位方向：`{jobDirection}`
- 岗位要求：`{jobRequirements}`
- 用户目标方向：`{targetDirection}`
- 简历正文：

```text
{resumeText}
```

输出要求：

- `summary`：1 到 3 句总体诊断。
- `coreProblems`：2 到 4 条最重要的问题。
- `missingKeywords`：3 到 12 个岗位相关且简历中缺失或表达不足的关键词；没有可靠依据时返回空数组。
- `improvementSuggestions`：3 到 5 条可以直接执行的改进建议。
- 所有内容必须以输入为依据；无法判断时使用空字符串或空数组。
- 只返回 JSON，不要返回 Markdown、注释或额外说明。
