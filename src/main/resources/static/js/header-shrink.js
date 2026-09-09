(() => {
    const TOP_THRESHOLD = 28;
    const HIDE_THRESHOLD = 96;
    const DIRECTION_DELTA = 6;

    const initNavbarMotion = () => {
        const navbarShell = document.querySelector("[data-navbar-shell]");
        if (!navbarShell) {
            return;
        }

        const floatingNav = navbarShell.querySelector("[data-floating-nav]");
        const toggleButton = navbarShell.querySelector("[data-quick-nav-toggle]");
        const quickMenu = navbarShell.querySelector("[data-quick-nav-menu]");
        let lastScrollY = window.scrollY || 0;
        let ticking = false;

        const setMenuOpen = (open) => {
            navbarShell.classList.toggle("is-menu-open", open);
            if (toggleButton) {
                toggleButton.setAttribute("aria-expanded", String(open));
            }
        };

        const closeMenu = () => setMenuOpen(false);

        const syncFloatingAccessibility = () => {
            if (!floatingNav) {
                return;
            }
            const floatingVisible = navbarShell.classList.contains("is-floating-visible");
            floatingNav.setAttribute("aria-hidden", String(!floatingVisible));
            if (!floatingVisible) {
                closeMenu();
            }
        };

        const syncHeaderState = () => {
            const currentScrollY = window.scrollY || 0;
            const delta = currentScrollY - lastScrollY;
            const isTop = currentScrollY <= TOP_THRESHOLD;
            const shouldHideHeader = currentScrollY > HIDE_THRESHOLD;
            const isScrollingDown = delta > DIRECTION_DELTA;
            const isScrollingUp = delta < -DIRECTION_DELTA;

            navbarShell.classList.toggle("is-top", isTop);

            if (isTop) {
                navbarShell.classList.remove("is-header-hidden", "is-floating-visible", "is-floating-strong");
            } else if (shouldHideHeader) {
                navbarShell.classList.add("is-header-hidden", "is-floating-visible");
                navbarShell.classList.toggle("is-floating-strong", isScrollingUp || currentScrollY < HIDE_THRESHOLD * 2);
                if (isScrollingDown) {
                    navbarShell.classList.remove("is-floating-strong");
                }
            } else {
                navbarShell.classList.remove("is-header-hidden", "is-floating-visible", "is-floating-strong");
            }

            syncFloatingAccessibility();
            lastScrollY = currentScrollY;
            ticking = false;
        };

        const requestSync = () => {
            if (ticking) {
                return;
            }
            ticking = true;
            window.requestAnimationFrame(syncHeaderState);
        };

        if (toggleButton) {
            toggleButton.addEventListener("click", (event) => {
                event.stopPropagation();
                const nextOpen = !navbarShell.classList.contains("is-menu-open");
                setMenuOpen(nextOpen);
            });
        }

        if (quickMenu) {
            quickMenu.querySelectorAll("a, button").forEach((node) => {
                node.addEventListener("click", closeMenu);
            });
        }

        document.addEventListener("click", (event) => {
            if (!navbarShell.contains(event.target)) {
                closeMenu();
            }
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeMenu();
            }
        });

        syncHeaderState();
        window.addEventListener("scroll", requestSync, {passive: true});
        window.addEventListener("resize", requestSync, {passive: true});
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initNavbarMotion, {once: true});
    } else {
        initNavbarMotion();
    }
})();
