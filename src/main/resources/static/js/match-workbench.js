(function () {
    const cards = Array.from(document.querySelectorAll("[data-match-card]"));
    const host = document.querySelector("[data-match-detail-host]");
    const listEmpty = document.querySelector("[data-match-filter-empty]");
    const detailEmpty = document.querySelector("[data-match-detail-empty]");
    const visibleCountNode = document.querySelector("[data-match-visible-count]");
    const totalCountNode = document.querySelector("[data-match-total-count]");

    if (!cards.length) {
        return;
    }

    const controls = {
        city: document.querySelector("[data-match-filter='city']"),
        salary: document.querySelector("[data-match-filter='salary']"),
        industry: document.querySelector("[data-match-filter='industry']"),
        nature: document.querySelector("[data-match-filter='nature']"),
        risk: document.querySelector("[data-match-filter='risk']")
    };

    let activeCard = cards.find((card) => card.classList.contains("is-active")) || null;

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

    function collectDatasetValues(key) {
        return Array.from(new Set(cards
            .map((card) => normalizeText(card.dataset[key]))
            .filter((value) => value && value !== "-")))
            .sort((left, right) => left.localeCompare(right, "zh-Hans-CN"));
    }

    function populateSelect(select, values) {
        if (!select) {
            return;
        }

        const existingValues = new Set(Array.from(select.options).map((option) => option.value));
        values.forEach((value) => {
            if (!value || existingValues.has(value)) {
                return;
            }
            const option = document.createElement("option");
            option.value = value;
            option.textContent = value;
            select.appendChild(option);
        });
    }

    function visibleCards() {
        return cards.filter((card) => !card.hidden);
    }

    function notifyAiProgressRefresh() {
        if (window.JobAgentAiProgress && typeof window.JobAgentAiProgress.init === "function") {
            window.JobAgentAiProgress.init();
            return;
        }
        document.dispatchEvent(new Event("ai-progress:refresh"));
    }

    function renderDetail(card) {
        if (!host) {
            return;
        }

        const template = card ? card.querySelector("[data-match-detail-template]") : null;
        host.innerHTML = template ? template.innerHTML : "";
        notifyAiProgressRefresh();
    }

    function toggleEmptyStates(hasVisibleCards) {
        if (listEmpty) {
            listEmpty.hidden = hasVisibleCards;
        }
        if (detailEmpty) {
            detailEmpty.hidden = hasVisibleCards;
        }
    }

    function setActiveCard(card, scrollIntoView) {
        activeCard = card || null;
        cards.forEach((item) => {
            item.classList.toggle("is-active", item === activeCard);
        });

        if (!activeCard) {
            renderDetail(null);
            toggleEmptyStates(false);
            return;
        }

        renderDetail(activeCard);
        toggleEmptyStates(true);
        if (scrollIntoView) {
            activeCard.scrollIntoView({behavior: "smooth", block: "nearest"});
        }
    }

    function applyFilters() {
        const cityValue = normalizeText(controls.city && controls.city.value);
        const salaryValue = normalizeText(controls.salary && controls.salary.value);
        const industryValue = normalizeText(controls.industry && controls.industry.value);
        const natureValue = normalizeText(controls.nature && controls.nature.value);
        const riskValue = normalizeText(controls.risk && controls.risk.value);

        let visibleCount = 0;

        cards.forEach((card) => {
            const city = normalizeText(card.dataset.city);
            const industry = normalizeText(card.dataset.industry);
            const nature = normalizeText(card.dataset.nature);
            const risk = normalizeText(card.dataset.risk);
            const salaryUnit = card.dataset.salaryUnit || "";
            const salaryMin = parseAmount(card.dataset.salaryMin);
            const salaryMax = parseAmount(card.dataset.salaryMax);

            const matched = (!cityValue || city === cityValue)
                && (!industryValue || industry === industryValue)
                && (!natureValue || nature === natureValue)
                && (!riskValue || risk === riskValue)
                && salaryMatches(salaryValue, salaryUnit, salaryMin, salaryMax);

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

        const currentVisibleCards = visibleCards();
        if (!currentVisibleCards.length) {
            setActiveCard(null, false);
            return;
        }

        if (!activeCard || activeCard.hidden) {
            setActiveCard(currentVisibleCards[0], false);
            return;
        }

        toggleEmptyStates(true);
    }

    function resetFilters() {
        Object.values(controls).forEach((control) => {
            if (control) {
                control.value = "";
            }
        });
        applyFilters();
    }

    function bindCard(card) {
        card.addEventListener("click", function () {
            setActiveCard(card, false);
        });

        card.addEventListener("keydown", function (event) {
            if (event.key !== "Enter" && event.key !== " ") {
                return;
            }
            event.preventDefault();
            setActiveCard(card, false);
        });
    }

    populateSelect(controls.city, collectDatasetValues("city"));
    populateSelect(controls.industry, collectDatasetValues("industry"));
    populateSelect(controls.nature, collectDatasetValues("nature"));

    cards.forEach(bindCard);

    Object.values(controls).forEach((control) => {
        if (!control) {
            return;
        }
        control.addEventListener("input", applyFilters);
        control.addEventListener("change", applyFilters);
    });

    document.querySelectorAll("[data-match-filter-reset]").forEach((button) => {
        button.addEventListener("click", resetFilters);
    });

    applyFilters();
})();
