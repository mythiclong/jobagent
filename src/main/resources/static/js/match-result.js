(function () {
    const filterRoot = document.querySelector("[data-match-filter-form]");
    const cards = Array.from(document.querySelectorAll("[data-match-card]"));
    const emptyState = document.querySelector("[data-match-filter-empty]");
    const visibleCountNode = document.querySelector("[data-match-visible-count]");
    const totalCountNode = document.querySelector("[data-match-total-count]");

    if (!cards.length) {
        return;
    }

    const controls = {
        city: document.querySelector("[data-match-filter='city']"),
        salary: document.querySelector("[data-match-filter='salary']"),
        education: document.querySelector("[data-match-filter='education']"),
        nature: document.querySelector("[data-match-filter='nature']"),
        source: document.querySelector("[data-match-filter='source']"),
        keyword: document.querySelector("[data-match-filter='keyword']")
    };

    function normalizeText(value) {
        return (value || "").toString().trim().toLowerCase();
    }

    function parseAmount(value) {
        if (value === undefined || value === null || value === "") {
            return null;
        }
        const amount = Number.parseFloat(value);
        return Number.isFinite(amount) ? amount : null;
    }

    function populateDynamicOptions(select, values) {
        if (!select) {
            return;
        }

        const existingValues = new Set(Array.from(select.options).map((option) => option.value));
        values.forEach((value) => {
            if (!value || value === "-" || existingValues.has(value)) {
                return;
            }
            const option = document.createElement("option");
            option.value = value;
            option.textContent = value;
            select.appendChild(option);
        });
    }

    function collectDatasetValues(key) {
        return Array.from(new Set(cards
            .map((card) => normalizeText(card.dataset[key]))
            .filter((value) => value && value !== "-")))
            .sort((left, right) => left.localeCompare(right, "zh-Hans-CN"));
    }

    function salaryMatches(rangeKey, salaryUnit, salaryMin, salaryMax) {
        if (!rangeKey) {
            return true;
        }

        if ((salaryUnit || "").toUpperCase() !== "MONTH") {
            return false;
        }

        if (salaryMin === null && salaryMax === null) {
            return false;
        }

        const effectiveMin = salaryMin !== null ? salaryMin : salaryMax;
        const effectiveMax = salaryMax !== null ? salaryMax : salaryMin;
        const lowerBound = effectiveMin !== null ? effectiveMin : effectiveMax;
        const upperBound = effectiveMax !== null ? effectiveMax : effectiveMin;

        switch (rangeKey) {
            case "lt3000":
                return upperBound !== null && upperBound < 3000;
            case "3000-5000":
                return lowerBound !== null && upperBound !== null && lowerBound <= 5000 && upperBound >= 3000;
            case "5000-8000":
                return lowerBound !== null && upperBound !== null && lowerBound <= 8000 && upperBound >= 5000;
            case "8000-12000":
                return lowerBound !== null && upperBound !== null && lowerBound <= 12000 && upperBound >= 8000;
            case "gte12000":
                return upperBound !== null && upperBound >= 12000;
            default:
                return true;
        }
    }

    function keywordMatches(query, haystack) {
        if (!query) {
            return true;
        }

        const normalizedHaystack = normalizeText(haystack);
        const terms = query.split(/\s+/).filter(Boolean);
        return terms.every((term) => normalizedHaystack.includes(term));
    }

    function applyFilters() {
        const cityValue = normalizeText(controls.city && controls.city.value);
        const salaryValue = normalizeText(controls.salary && controls.salary.value);
        const educationValue = normalizeText(controls.education && controls.education.value);
        const natureValue = normalizeText(controls.nature && controls.nature.value);
        const sourceValue = normalizeText(controls.source && controls.source.value);
        const keywordValue = normalizeText(controls.keyword && controls.keyword.value);

        let visibleCount = 0;

        cards.forEach((card) => {
            const city = normalizeText(card.dataset.city);
            const education = normalizeText(card.dataset.education);
            const nature = normalizeText(card.dataset.nature);
            const sourceType = normalizeText(card.dataset.sourceType);
            const keywords = card.dataset.keywords || "";
            const salaryUnit = card.dataset.salaryUnit || "";
            const salaryMin = parseAmount(card.dataset.salaryMin);
            const salaryMax = parseAmount(card.dataset.salaryMax);

            const matched = (!cityValue || city.includes(cityValue))
                && (!educationValue || education === educationValue)
                && (!natureValue || nature === natureValue)
                && (!sourceValue || sourceType === sourceValue)
                && salaryMatches(salaryValue, salaryUnit, salaryMin, salaryMax)
                && keywordMatches(keywordValue, keywords);

            card.hidden = !matched;
            if (matched) {
                visibleCount += 1;
            }
        });

        if (visibleCountNode) {
            visibleCountNode.textContent = String(visibleCount);
        }
        if (totalCountNode) {
            totalCountNode.textContent = String(cards.length);
        }
        if (emptyState) {
            emptyState.hidden = visibleCount !== 0;
        }
    }

    function bindDetailToggle(button) {
        button.addEventListener("click", function () {
            const card = button.closest("[data-match-card]");
            if (!card) {
                return;
            }

            const isExpanded = card.classList.toggle("is-detail-expanded");
            const label = isExpanded ? "收起详情" : "展开详情";
            card.querySelectorAll("[data-match-detail-toggle]").forEach((toggle) => {
                toggle.textContent = label;
                toggle.setAttribute("aria-expanded", String(isExpanded));
            });
        });
    }

    function bindSummaryToggle(button) {
        button.addEventListener("click", function () {
            const summaryBlock = button.closest(".match-summary-block");
            if (!summaryBlock) {
                return;
            }

            const isExpanded = summaryBlock.classList.toggle("is-summary-expanded");
            button.textContent = isExpanded ? "收起摘要" : "展开摘要";
            button.setAttribute("aria-expanded", String(isExpanded));
        });
    }

    function resetFilters() {
        Object.values(controls).forEach((control) => {
            if (!control) {
                return;
            }
            control.value = "";
        });
        applyFilters();
    }

    populateDynamicOptions(controls.education, collectDatasetValues("education"));
    populateDynamicOptions(controls.nature, collectDatasetValues("nature"));

    if (filterRoot) {
        filterRoot.querySelectorAll("input, select").forEach((control) => {
            control.addEventListener("input", applyFilters);
            control.addEventListener("change", applyFilters);
        });
    }

    document.querySelectorAll("[data-match-filter-reset]").forEach((button) => {
        button.addEventListener("click", resetFilters);
    });

    document.querySelectorAll("[data-match-detail-toggle]").forEach(bindDetailToggle);
    document.querySelectorAll("[data-match-summary-toggle]").forEach(bindSummaryToggle);

    applyFilters();
})();
