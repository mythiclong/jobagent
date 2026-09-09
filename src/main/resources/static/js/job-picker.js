(() => {
    function normalizeText(value) {
        return (value || "").trim().toLowerCase();
    }

    function tokenize(value) {
        return normalizeText(value).split(/\s+/).filter(Boolean);
    }

    function buildOptions(select) {
        return Array.from(select.options)
            .filter((option) => option.value)
            .map((option, index) => ({
                value: option.value,
                label: option.text.trim(),
                searchText: normalizeText(option.dataset.searchText || option.text),
                index
            }));
    }

    function rankOption(option, tokens) {
        let score = 0;
        for (const token of tokens) {
            if (option.searchText.startsWith(token)) {
                score += 80;
            }
            if (option.searchText.includes(token)) {
                score += 28;
            }
            if (option.label.toLowerCase().startsWith(token)) {
                score += 18;
            }
        }
        return score;
    }

    function updateSelectionState(select, selectedLabelEl) {
        const selectedOption = select.options[select.selectedIndex];
        if (selectedOption && selectedOption.value) {
            selectedLabelEl.textContent = `当前已选：${selectedOption.text.trim()}`;
            return;
        }
        selectedLabelEl.textContent = "当前还没有选择目标岗位。";
    }

    function updateFeedback(feedbackEl, query, matchCount, hasSelection) {
        if (query) {
            feedbackEl.textContent = matchCount > 0
                ? `已找到 ${matchCount} 个匹配岗位，先点结果，再确认选择。`
                : "没有找到匹配岗位，可以清空后重搜，或展开完整列表手动选择。";
            return;
        }
        feedbackEl.textContent = hasSelection
            ? "可以继续搜索切换岗位，没搜到再展开完整列表手动选择。"
            : "先搜索岗位名、公司名或方向，再从结果里确认岗位。";
    }

    function renderResults(wrapper) {
        const query = wrapper.searchInput.value.trim();
        const tokens = tokenize(query);
        const selectedValue = wrapper.select.value;
        const resultsEl = wrapper.resultsEl;

        resultsEl.innerHTML = "";
        if (tokens.length === 0) {
            resultsEl.hidden = true;
            updateSelectionState(wrapper.select, wrapper.selectedLabelEl);
            updateFeedback(wrapper.feedbackEl, "", 0, !!selectedValue);
            return;
        }

        const matches = wrapper.jobOptions
            .filter((option) => tokens.every((token) => option.searchText.includes(token)))
            .map((option) => ({ ...option, score: rankOption(option, tokens) }))
            .sort((left, right) => right.score - left.score || left.index - right.index);

        resultsEl.hidden = false;
        if (matches.length === 0) {
            const emptyEl = document.createElement("div");
            emptyEl.className = "job-search-empty";
            emptyEl.textContent = "没有搜到匹配岗位，可以换个关键词，或展开完整岗位列表手动选择。";
            resultsEl.appendChild(emptyEl);
        } else {
            matches.slice(0, 8).forEach((option) => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "job-search-result";
                if (selectedValue === option.value) {
                    button.classList.add("is-selected");
                }
                button.textContent = option.label;
                button.addEventListener("click", () => {
                    wrapper.select.value = option.value;
                    wrapper.searchInput.value = "";
                    renderResults(wrapper);
                });
                resultsEl.appendChild(button);
            });
        }

        updateSelectionState(wrapper.select, wrapper.selectedLabelEl);
        updateFeedback(wrapper.feedbackEl, query, matches.length, !!selectedValue);
    }

    function bindFormValidation(wrapper) {
        const form = wrapper.select.closest("form");
        if (!form || !wrapper.select.required) {
            return;
        }
        form.addEventListener("submit", (event) => {
            if (wrapper.select.value) {
                return;
            }
            event.preventDefault();
            wrapper.manualEl.open = true;
            wrapper.feedbackEl.textContent = "请先搜索并选择目标岗位，或展开完整列表手动选择。";
            wrapper.searchInput.focus();
        });
    }

    function initJobPicker(root) {
        const select = root.querySelector(".job-search-select");
        const searchInput = root.querySelector(".job-search-input");
        const resultsEl = root.querySelector(".job-search-results");
        const feedbackEl = root.querySelector(".job-search-feedback");
        const selectedLabelEl = root.querySelector(".job-search-selected");
        const clearButton = root.querySelector(".job-search-clear");
        const manualEl = root.querySelector(".job-search-manual");

        if (!select || !searchInput || !resultsEl || !feedbackEl || !selectedLabelEl || !manualEl) {
            return;
        }

        const wrapper = {
            select,
            searchInput,
            resultsEl,
            feedbackEl,
            selectedLabelEl,
            manualEl,
            jobOptions: buildOptions(select)
        };

        searchInput.addEventListener("input", () => renderResults(wrapper));
        select.addEventListener("change", () => renderResults(wrapper));
        clearButton?.addEventListener("click", () => {
            searchInput.value = "";
            renderResults(wrapper);
            searchInput.focus();
        });

        bindFormValidation(wrapper);
        renderResults(wrapper);
    }

    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll(".job-search-picker").forEach(initJobPicker);
    });
})();
