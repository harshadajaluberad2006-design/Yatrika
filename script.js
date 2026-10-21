
document.addEventListener("DOMContentLoaded", function () {

    // ==========================================
    // 1. MOBILE NAVIGATION
    // ==========================================

    const menuToggle = document.querySelector(".menu-toggle");
    const navLinks = document.querySelector(".nav-links");

    if (menuToggle && navLinks) {
        menuToggle.addEventListener("click", function () {
            const isOpen = navLinks.classList.toggle("show");

            menuToggle.setAttribute("aria-expanded", String(isOpen));
            menuToggle.textContent = isOpen ? "✕" : "☰";
        });

        navLinks.querySelectorAll("a").forEach(function (link) {
            link.addEventListener("click", function () {
                navLinks.classList.remove("show");
                menuToggle.setAttribute("aria-expanded", "false");
                menuToggle.textContent = "☰";
            });
        });
    }


    // ==========================================
    // 2. MINIMUM TRAVEL DATE
    // ==========================================

    const dateInput = document.querySelector('input[name="date"]');

    if (dateInput) {
        const today = new Date();
        const localToday = [
            today.getFullYear(),
            String(today.getMonth() + 1).padStart(2, "0"),
            String(today.getDate()).padStart(2, "0")
        ].join("-");

        dateInput.min = localToday;
    }


    // ==========================================
    // 3. SELECT PACKAGE FROM URL
    // ==========================================

    const urlParams = new URLSearchParams(window.location.search);
    const selectedPackage =
        urlParams.get("package") || urlParams.get("destination");

    const destinationSelect =
        document.querySelector('select[name="destination"]');

    if (destinationSelect && selectedPackage) {
        const matchingOption = Array.from(destinationSelect.options).find(
            function (option) {
                return option.value.toLowerCase() ===
                           selectedPackage.toLowerCase() ||
                       option.textContent.trim().toLowerCase() ===
                           selectedPackage.toLowerCase();
            }
        );

        if (matchingOption) {
            destinationSelect.value = matchingOption.value;
        }
    }


    // ==========================================
    // 4. ADVENTURE CARD LINKS
    // ==========================================

    document.querySelectorAll(".adventure-card").forEach(function (card) {
        const link = card.querySelector('a[href*="contact.html"]');

        if (link && !link.hasAttribute("data-yatrika-handled")) {
            link.setAttribute("data-yatrika-handled", "true");

            link.addEventListener("click", function (event) {
                const heading = card.querySelector("h2, h3, h4");
                const title = heading ? heading.textContent.trim() : "";

                if (title) {
                    event.preventDefault();

                    const target = new URL(
                        link.getAttribute("href"),
                        window.location.href
                    );

                    target.searchParams.set("package", title);
                    window.location.href = target.href;
                }
            });
        }
    });


    // ==========================================
    // 5. CONTACT FORM - SAVE TO MYSQL
    // ==========================================

    const journeyForm = document.getElementById("journeyForm");
    const formMessage = document.getElementById("formMessage");

    if (journeyForm) {
        journeyForm.addEventListener("submit", async function (event) {
            event.preventDefault();

            if (!journeyForm.checkValidity()) {
                journeyForm.reportValidity();
                return;
            }

            const submitButton = journeyForm.querySelector(
                'button[type="submit"], input[type="submit"]'
            );

            const originalButtonText = submitButton
                ? submitButton.textContent
                : "";

            if (submitButton) {
                submitButton.disabled = true;
                submitButton.textContent = "Sending...";
            }

            if (formMessage) {
                formMessage.textContent = "";
                formMessage.style.color = "";
            }

            try {
                // Read all fields from the existing contact form.
                const formData = new FormData(journeyForm);

                // IMPORTANT:
                // The Java backend expects
                // application/x-www-form-urlencoded, not JSON.
                const encodedData = new URLSearchParams();

                formData.forEach(function (value, key) {
                    encodedData.append(key, value);
                });

                const response = await fetch("/api/enquiries", {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/x-www-form-urlencoded;charset=UTF-8"
                    },
                    body: encodedData.toString()
                });

                const result = await response.json();

                if (!response.ok || result.success !== true) {
                    throw new Error(
                        result.message || "Your enquiry could not be saved."
                    );
                }

                if (formMessage) {
                    formMessage.textContent =
                        result.message ||
                        "Thank you! Your travel enquiry has been submitted successfully.";

                    formMessage.style.color = "green";
                }

                journeyForm.reset();

                // Reset the minimum date after clearing the form.
                if (dateInput) {
                    const today = new Date();
                    dateInput.min = [
                        today.getFullYear(),
                        String(today.getMonth() + 1).padStart(2, "0"),
                        String(today.getDate()).padStart(2, "0")
                    ].join("-");
                }

            } catch (error) {
                console.error("Yatrika enquiry error:", error);

                if (formMessage) {
                    formMessage.textContent =
                        error.message ||
                        "Sorry, your enquiry could not be saved. Please try again.";

                    formMessage.style.color = "red";
                }

            } finally {
                if (submitButton) {
                    submitButton.disabled = false;

                    if (submitButton.tagName === "INPUT") {
                        submitButton.value = originalButtonText;
                    } else {
                        submitButton.textContent = originalButtonText;
                    }
                }
            }
        });
    }


    // ==========================================
    // 6. AUTOMATIC FOOTER YEAR
    // ==========================================

    document.querySelectorAll(".footer-bottom p").forEach(function (paragraph) {
        if (paragraph.textContent.includes("Yatrika")) {
            paragraph.innerHTML = paragraph.innerHTML.replace(
                /©\s*\d{4}/,
                "© " + new Date().getFullYear()
            );
        }
    });

});
