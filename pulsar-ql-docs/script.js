// Pulsar QL Documentation JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // Initialize all functionality
    initializeScrollToTop();
    initializeSmoothScrolling();
    initializeFunctionSearch();
    initializeCodeCopyButtons();
    initializeSidebarNavigation();
    initializeTooltips();

    // Add loading animation completion
    document.body.classList.add('loaded');
});

// Scroll to Top Button
function initializeScrollToTop() {
    const scrollButton = document.createElement('button');
    scrollButton.className = 'scroll-to-top';
    scrollButton.innerHTML = '↑';
    scrollButton.setAttribute('aria-label', 'Scroll to top');
    document.body.appendChild(scrollButton);

    // Show/hide button based on scroll position
    window.addEventListener('scroll', function() {
        if (window.pageYOffset > 300) {
            scrollButton.classList.add('visible');
        } else {
            scrollButton.classList.remove('visible');
        }
    });

    // Scroll to top when clicked
    scrollButton.addEventListener('click', function() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
}

// Smooth Scrolling for Navigation Links
function initializeSmoothScrolling() {
    const navLinks = document.querySelectorAll('a[href^="#"]');

    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();

            const targetId = this.getAttribute('href').substring(1);
            const targetElement = document.getElementById(targetId);

            if (targetElement) {
                const offsetTop = targetElement.offsetTop - 100; // Account for fixed header

                window.scrollTo({
                    top: offsetTop,
                    behavior: 'smooth'
                });

                // Update URL without jumping
                history.pushState(null, null, '#' + targetId);
            }
        });
    });
}

// Function Search Functionality
function initializeFunctionSearch() {
    // Add search input if it doesn't exist
    const mainContent = document.querySelector('main');
    if (mainContent && document.getElementById('functions')) {
        const searchContainer = document.createElement('div');
        searchContainer.className = 'function-search-container mb-4';
        searchContainer.innerHTML = `
            <div class="input-group">
                <span class="input-group-text">
                    <svg width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
                        <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/>
                    </svg>
                </span>
                <input type="text" class="form-control" id="functionSearch" placeholder="Search functions...">
                <button class="btn btn-outline-secondary" type="button" id="clearSearch">Clear</button>
            </div>
        `;

        const functionsSection = document.getElementById('functions');
        if (functionsSection) {
            functionsSection.insertBefore(searchContainer, functionsSection.firstChild);
        }

        const searchInput = document.getElementById('functionSearch');
        const clearButton = document.getElementById('clearSearch');

        if (searchInput) {
            searchInput.addEventListener('input', function() {
                const searchTerm = this.value.toLowerCase();
                filterFunctions(searchTerm);
            });
        }

        if (clearButton) {
            clearButton.addEventListener('click', function() {
                if (searchInput) {
                    searchInput.value = '';
                    filterFunctions('');
                    searchInput.focus();
                }
            });
        }
    }
}

// Filter functions based on search term
function filterFunctions(searchTerm) {
    const functionItems = document.querySelectorAll('.function-item');
    let visibleCount = 0;

    functionItems.forEach(item => {
        const title = item.querySelector('h5').textContent.toLowerCase();
        const description = item.querySelector('p').textContent.toLowerCase();
        const code = item.querySelector('code') ? item.querySelector('code').textContent.toLowerCase() : '';

        const matches = title.includes(searchTerm) ||
                       description.includes(searchTerm) ||
                       code.includes(searchTerm);

        if (matches || searchTerm === '') {
            item.style.display = 'block';
            visibleCount++;

            // Highlight matching text
            if (searchTerm !== '') {
                highlightText(item, searchTerm);
            } else {
                removeHighlight(item);
            }
        } else {
            item.style.display = 'none';
        }
    });

    // Update search results count
    updateSearchResults(visibleCount, searchTerm);
}

// Highlight matching text
function highlightText(element, searchTerm) {
    const walker = document.createTreeWalker(
        element,
        NodeFilter.SHOW_TEXT,
        null,
        false
    );

    const textNodes = [];
    let node;

    while (node = walker.nextNode()) {
        if (node.textContent.toLowerCase().includes(searchTerm)) {
            textNodes.push(node);
        }
    }

    textNodes.forEach(textNode => {
        const text = textNode.textContent;
        const regex = new RegExp(`(${searchTerm})`, 'gi');
        const highlightedText = text.replace(regex, '<span class="highlight">$1</span>');

        const span = document.createElement('span');
        span.innerHTML = highlightedText;
        textNode.parentNode.replaceChild(span, textNode);
    });
}

// Remove highlighting
function removeHighlight(element) {
    const highlights = element.querySelectorAll('.highlight');
    highlights.forEach(highlight => {
        const parent = highlight.parentNode;
        parent.replaceChild(document.createTextNode(highlight.textContent), highlight);
        parent.normalize();
    });
}

// Update search results count
function updateSearchResults(count, searchTerm) {
    let resultsInfo = document.getElementById('searchResultsInfo');

    if (!resultsInfo) {
        resultsInfo = document.createElement('div');
        resultsInfo.id = 'searchResultsInfo';
        resultsInfo.className = 'search-results-info mt-3';

        const searchContainer = document.querySelector('.function-search-container');
        if (searchContainer) {
            searchContainer.appendChild(resultsInfo);
        }
    }

    if (searchTerm === '') {
        resultsInfo.style.display = 'none';
    } else {
        resultsInfo.style.display = 'block';
        resultsInfo.innerHTML = `<small class="text-muted">Found ${count} function${count !== 1 ? 's' : ''} matching "${searchTerm}"</small>`;
    }
}

// Copy Code Buttons
function initializeCodeCopyButtons() {
    const codeBlocks = document.querySelectorAll('pre[class*="language-"]');

    codeBlocks.forEach(block => {
        const button = document.createElement('button');
        button.className = 'btn btn-sm btn-outline-secondary copy-code-btn';
        button.innerHTML = 'Copy';
        button.style.position = 'absolute';
        button.style.top = '0.5rem';
        button.style.right = '0.5rem';
        button.style.opacity = '0';
        button.style.transition = 'opacity 0.3s ease';

        // Make the pre element relative for absolute positioning
        block.style.position = 'relative';
        block.appendChild(button);

        // Show button on hover
        block.addEventListener('mouseenter', () => {
            button.style.opacity = '1';
        });

        block.addEventListener('mouseleave', () => {
            button.style.opacity = '0';
        });

        button.addEventListener('click', async () => {
            const code = block.querySelector('code').textContent;

            try {
                await navigator.clipboard.writeText(code);
                button.innerHTML = 'Copied!';
                button.classList.remove('btn-outline-secondary');
                button.classList.add('btn-success');

                setTimeout(() => {
                    button.innerHTML = 'Copy';
                    button.classList.remove('btn-success');
                    button.classList.add('btn-outline-secondary');
                }, 2000);
            } catch (err) {
                console.error('Failed to copy code:', err);
                button.innerHTML = 'Failed';

                setTimeout(() => {
                    button.innerHTML = 'Copy';
                }, 2000);
            }
        });
    });
}

// Sidebar Navigation
function initializeSidebarNavigation() {
    const sidebarLinks = document.querySelectorAll('.sidebar .nav-link');
    const sections = document.querySelectorAll('section[id]');

    // Update active link based on scroll position
    function updateActiveLink() {
        let current = '';

        sections.forEach(section => {
            const sectionTop = section.offsetTop - 120;
            const sectionHeight = section.clientHeight;

            if (window.pageYOffset >= sectionTop && window.pageYOffset < sectionTop + sectionHeight) {
                current = section.getAttribute('id');
            }
        });

        sidebarLinks.forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('href') === '#' + current) {
                link.classList.add('active');
            }
        });
    }

    window.addEventListener('scroll', updateActiveLink);
    updateActiveLink(); // Initial call
}

// Tooltips
function initializeTooltips() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

// Function Category Navigation
function initializeFunctionCategoryNavigation() {
    const categoryCards = document.querySelectorAll('.function-category-card');

    categoryCards.forEach(card => {
        card.addEventListener('click', function() {
            const category = this.getAttribute('data-category');
            const targetSection = document.getElementById(category + '-functions');

            if (targetSection) {
                const offsetTop = targetSection.offsetTop - 100;

                window.scrollTo({
                    top: offsetTop,
                    behavior: 'smooth'
                });
            }
        });
    });
}

// Responsive Navigation
function initializeResponsiveNavigation() {
    const navbarToggler = document.querySelector('.navbar-toggler');

    if (navbarToggler) {
        navbarToggler.addEventListener('click', function() {
            // Add any custom mobile navigation behavior here
            console.log('Mobile menu toggled');
        });
    }
}

// Keyboard Navigation
function initializeKeyboardNavigation() {
    document.addEventListener('keydown', function(e) {
        // Alt + / to focus search
        if (e.altKey && e.key === '/') {
            e.preventDefault();
            const searchInput = document.getElementById('functionSearch');
            if (searchInput) {
                searchInput.focus();
            }
        }

        // Escape to clear search
        if (e.key === 'Escape') {
            const searchInput = document.getElementById('functionSearch');
            if (searchInput && searchInput.value) {
                searchInput.value = '';
                filterFunctions('');
            }
        }
    });
}

// Performance Optimization: Debounce function
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Initialize additional features
function initializeAdditionalFeatures() {
    initializeFunctionCategoryNavigation();
    initializeResponsiveNavigation();
    initializeKeyboardNavigation();
}

// Call additional initialization
document.addEventListener('DOMContentLoaded', initializeAdditionalFeatures);