(function () {
    function createOverlay() {
        let overlay = document.getElementById("ai-progress-overlay");
        if (overlay) {
            return overlay;
        }

        overlay = document.createElement("div");
        overlay.id = "ai-progress-overlay";
        overlay.className = "ai-progress-overlay hidden";
        overlay.innerHTML = `
            <div class="ai-progress-card">
                <div class="ai-progress-head">
                    <h3 class="ai-progress-title" id="ai-progress-title">正在处理请求</h3>
                    <span class="status-badge status-qwen" id="ai-progress-badge">进行中</span>
                </div>
                <p class="ai-progress-stage" id="ai-progress-stage">正在提交请求...</p>
                <div class="ai-progress-bar">
                    <div class="ai-progress-fill" id="ai-progress-fill"></div>
                </div>
                <div class="ai-progress-meta">
                    <span class="ai-progress-percent" id="ai-progress-percent">0%</span>
                    <span class="ai-progress-hint" id="ai-progress-hint">请稍候，结果生成后会自动返回当前页面。</span>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);
        return overlay;
    }

    function parseSteps(value) {
        if (!value) {
            return [
                "提交请求",
                "整理当前信息",
                "生成结果",
                "准备返回页面"
            ];
        }
        return value.split("|").map((item) => item.trim()).filter((item) => item.length > 0);
    }

    function bindForm(form) {
        if (form.dataset.aiProgressBound === "true") {
            return;
        }
        form.dataset.aiProgressBound = "true";

        form.addEventListener("submit", function () {
            const overlay = createOverlay();
            const title = overlay.querySelector("#ai-progress-title");
            const stage = overlay.querySelector("#ai-progress-stage");
            const fill = overlay.querySelector("#ai-progress-fill");
            const percent = overlay.querySelector("#ai-progress-percent");
            const hint = overlay.querySelector("#ai-progress-hint");

            const steps = parseSteps(form.dataset.aiProgressSteps);
            const label = form.dataset.aiProgressLabel || "正在处理请求";

            title.textContent = label;
            stage.textContent = steps[0] || "正在处理请求...";
            fill.style.width = "6%";
            percent.textContent = "6%";
            hint.textContent = "请稍候，结果生成后会自动返回当前页面。";
            overlay.classList.remove("hidden");

            const submitButtons = form.querySelectorAll("button[type='submit'],input[type='submit']");
            submitButtons.forEach((btn) => {
                btn.disabled = true;
                btn.dataset.originText = btn.textContent || "";
                if (btn.tagName.toLowerCase() === "button") {
                    btn.textContent = "处理中...";
                }
            });

            let progress = 6;
            let stepIndex = 0;
            const progressTimer = window.setInterval(() => {
                if (progress >= 92) {
                    return;
                }
                const increment = progress < 35 ? 9 : (progress < 70 ? 6 : 3);
                progress = Math.min(progress + increment, 92);
                fill.style.width = progress + "%";
                percent.textContent = progress + "%";
            }, 700);

            const stageTimer = window.setInterval(() => {
                stepIndex = Math.min(stepIndex + 1, steps.length - 1);
                stage.textContent = steps[stepIndex];
            }, 1400);

            window.setTimeout(() => {
                hint.textContent = "如果等待时间较长，请继续保持页面开启。";
            }, 9000);

            window.setTimeout(() => {
                if (!overlay.classList.contains("hidden")) {
                    hint.textContent = "请求仍在处理中，请不要重复提交。";
                }
            }, 16000);

            window.addEventListener("beforeunload", function () {
                window.clearInterval(progressTimer);
                window.clearInterval(stageTimer);
            }, {once: true});
        });
    }

    function init() {
        const forms = document.querySelectorAll("form[data-ai-progress='true']");
        forms.forEach(bindForm);
    }

    window.JobAgentAiProgress = {
        init: init
    };

    document.addEventListener("ai-progress:refresh", init);

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
