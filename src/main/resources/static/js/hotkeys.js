// src/main/resources/static/js/hotkeys.js

// Глобальные функции для Thymeleaf
function toggleHotkeys() {
    const panel = document.getElementById('hotkeysPanel');
    if (panel.style.display === 'block') {
        hideHotkeys();
    } else {
        showHotkeys();
    }
}

function hideHotkeys() {
    const panel = document.getElementById('hotkeysPanel');
    panel.style.display = 'none';
}

function showHotkeys() {
    const panel = document.getElementById('hotkeysPanel');
    panel.style.display = 'block';
}

// Основной класс горячих клавиш
class AdminHotkeys {
    constructor() {
        this.isPanelVisible = false;
        this.gKeyPressed = false;
        this.init();
    }

    init() {
        console.log('🎮 Горячие клавиши админ-панели активированы');
        this.bindEvents();

        // Показываем кнопку только для админов
        this.showForAdminsOnly();
    }

    showForAdminsOnly() {
        // Проверяем, есть ли элементы админ-панели на странице
        const isAdminPage = document.querySelector('[sec\\:authorize="hasRole(\\'ADMIN\\')"]') ||
                           window.location.pathname.includes('/admin') ||
                           window.location.pathname.includes('/users') ||
                           window.location.pathname.includes('/products') ||
                           window.location.pathname.includes('/orders') ||
                           window.location.pathname.includes('/categories') ||
                           window.location.pathname.includes('/brands');

        const trigger = document.getElementById('hotkeysTrigger');
        if (trigger && isAdminPage) {
            trigger.style.display = 'flex';
        } else if (trigger) {
            trigger.style.display = 'none';
        }
    }

    bindEvents() {
        // Глобальные горячие клавиши
        document.addEventListener('keydown', (e) => this.handleKeyDown(e));
        document.addEventListener('keyup', (e) => this.handleKeyUp(e));

        // Клики по элементам UI
        document.addEventListener('click', (e) => {
            if (e.target.closest('.hotkeys-overlay') ||
                e.target.closest('.hotkeys-close')) {
                this.hidePanel();
            }
            if (e.target.closest('#hotkeysTrigger')) {
                this.togglePanel();
            }
        });

        // Закрытие по ESC
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.isPanelVisible) {
                this.hidePanel();
            }
        });
    }

    handleKeyDown(e) {
        // Игнорируем ввод в полях ввода
        if (this.isInputField(e.target)) return;

        const key = e.key.toLowerCase();

        // Показать/скрыть панель
        if (key === '?' && !e.ctrlKey && !e.metaKey) {
            e.preventDefault();
            this.togglePanel();
            return;
        }

        // Навигация с G-префиксом
        if (key === 'g' && !this.gKeyPressed) {
            this.gKeyPressed = true;
            setTimeout(() => { this.gKeyPressed = false; }, 2000);
            return;
        }

        if (this.gKeyPressed) {
            e.preventDefault();
            this.handleNavigation(key);
            this.gKeyPressed = false;
            return;
        }

        // Прямые горячие клавиши
        this.handleDirectKeys(key, e);
    }

    handleKeyUp(e) {
        if (e.key.toLowerCase() === 'g') {
            this.gKeyPressed = false;
        }
    }

    handleNavigation(key) {
        const routes = {
            'd': '/',                    // Dashboard (home)
            'p': '/products',           // Products
            'o': '/orders',             // Orders
            'u': '/users',              // Users
            'c': '/categories',         // Categories
            'b': '/brands',             // Brands
            'a': '/admin/statistics',   // Analytics
            's': '/audit'               // Audit
        };

        if (routes[key]) {
            window.location.href = routes[key];
            this.showQuickHint(`Переход: ${this.getRouteName(key)}`);
        }
    }

    handleDirectKeys(key, e) {
        const actions = {
            'c': () => this.createNew(),
            's': () => this.saveForm(e),
            'e': () => this.editItem(),
            'f': () => this.focusSearch(),
            'escape': () => this.handleEscape()
        };

        if (actions[key]) {
            e.preventDefault();
            actions[key]();
        }
    }

    createNew() {
        const createBtn = document.querySelector('a[href*="create"], .btn-primary');
        if (createBtn) {
            createBtn.click();
            this.showQuickHint('Создание нового элемента');
        } else {
            this.showQuickHint('Кнопка "Создать" не найдена');
        }
    }

    saveForm(e) {
        if (e.ctrlKey || e.metaKey) {
            const saveBtn = document.querySelector('button[type="submit"]');
            if (saveBtn) {
                e.preventDefault();
                saveBtn.click();
                this.showQuickHint('Форма сохранена');
            }
        } else {
            const saveBtn = document.querySelector('.btn-success');
            if (saveBtn) {
                saveBtn.click();
                this.showQuickHint('Сохранение...');
            }
        }
    }

    editItem() {
        const editBtn = document.querySelector('a[href*="edit"], .btn-warning');
        if (editBtn) {
            editBtn.click();
            this.showQuickHint('Редактирование');
        }
    }

    focusSearch() {
        const searchInput = document.querySelector('input[type="search"], input[name="search"]');
        if (searchInput) {
            searchInput.focus();
            searchInput.select();
            this.showQuickHint('Поиск активирован');
        }
    }

    handleEscape() {
        if (this.isPanelVisible) {
            this.hidePanel();
        }
    }

    // Вспомогательные методы
    isInputField(element) {
        return element.tagName === 'INPUT' ||
               element.tagName === 'TEXTAREA' ||
               element.tagName === 'SELECT' ||
               element.isContentEditable;
    }

    getRouteName(key) {
        const names = {
            'd': 'Дашборд', 'p': 'Товары', 'o': 'Заказы',
            'u': 'Пользователи', 'c': 'Категории', 'b': 'Бренды',
            'a': 'Статистика', 's': 'Аудит'
        };
        return names[key] || 'Неизвестно';
    }

    showQuickHint(message) {
        // Создаем быстрое уведомление
        const hint = document.createElement('div');
        hint.className = 'quick-hint';
        hint.textContent = message;
        hint.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #10b981;
            color: white;
            padding: 12px 20px;
            border-radius: 8px;
            z-index: 10000;
            font-weight: 500;
            box-shadow: 0 4px 12px rgba(0,0,0,0.2);
            animation: hintSlideIn 0.3s ease-out;
        `;

        // Добавляем анимации в стили
        if (!document.querySelector('#hint-animations')) {
            const style = document.createElement('style');
            style.id = 'hint-animations';
            style.textContent = `
                @keyframes hintSlideIn {
                    from { transform: translateX(100%); opacity: 0; }
                    to { transform: translateX(0); opacity: 1; }
                }
                @keyframes hintSlideOut {
                    from { transform: translateX(0); opacity: 1; }
                    to { transform: translateX(100%); opacity: 0; }
                }
            `;
            document.head.appendChild(style);
        }

        document.body.appendChild(hint);

        setTimeout(() => {
            hint.style.animation = 'hintSlideOut 0.3s ease-in';
            setTimeout(() => {
                if (hint.parentNode) {
                    hint.parentNode.removeChild(hint);
                }
            }, 300);
        }, 2000);
    }

    togglePanel() {
        this.isPanelVisible ? this.hidePanel() : this.showPanel();
    }

    showPanel() {
        const panel = document.getElementById('hotkeysPanel');
        if (panel) {
            panel.style.display = 'block';
            this.isPanelVisible = true;
        }
    }

    hidePanel() {
        const panel = document.getElementById('hotkeysPanel');
        if (panel) {
            panel.style.display = 'none';
            this.isPanelVisible = false;
        }
    }
}

// Инициализация когда DOM готов
document.addEventListener('DOMContentLoaded', () => {
    // Проверяем, что мы в админ-панели
    const isAdminPanel = document.querySelector('[sec\\:authorize="hasRole(\\'ADMIN\\')"]') ||
                        window.location.pathname.includes('/admin') ||
                        window.location.pathname.includes('/users') ||
                        window.location.pathname.includes('/products') ||
                        window.location.pathname.includes('/orders') ||
                        window.location.pathname.includes('/categories') ||
                        window.location.pathname.includes('/brands') ||
                        window.location.pathname.includes('/audit');

    if (isAdminPanel) {
        window.adminHotkeys = new AdminHotkeys();
        console.log('🔥 Горячие клавиши активированы для админ-панели');
    }
});