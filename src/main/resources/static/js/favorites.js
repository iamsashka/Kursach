class FavoritesManager {
    constructor() {
        this.csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        this.csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        this.favoritesMap = new Map();
        this.initialized = false;
    }

    async init() {
        if (this.initialized) return;

        const productIds = this.getAllProductIds();
        if (productIds.length > 0) {
            await this.loadFavoritesStatus(productIds);
            this.updateAllButtons();
        }
        this.initialized = true;
    }

    getAllProductIds() {
        const buttons = document.querySelectorAll('[data-product-id]');
        return Array.from(buttons).map(btn => parseInt(btn.dataset.productId));
    }

    async loadFavoritesStatus(productIds) {
        try {
            const response = await fetch('/api/favorites/check-multiple', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [this.csrfHeader]: this.csrfToken
                },
                body: JSON.stringify(productIds)
            });

            if (response.ok) {
                const favoritesMap = await response.json();
                this.favoritesMap = new Map(Object.entries(favoritesMap).map(([k, v]) => [parseInt(k), v]));
            }
        } catch (error) {
            console.error('Error loading favorites status:', error);
        }
    }

    updateAllButtons() {
        document.querySelectorAll('[data-product-id]').forEach(button => {
            this.updateButtonState(button);
        });
    }

    updateButtonState(button) {
        const productId = parseInt(button.dataset.productId);
        const isFavorite = this.favoritesMap.get(productId) || false;

        const icon = button.querySelector('i');
        if (isFavorite) {
            button.classList.add('active');
            icon.className = 'fas fa-heart';
            button.title = 'Удалить из избранного';
        } else {
            button.classList.remove('active');
            icon.className = 'fas fa-heart';
            button.title = 'Добавить в избранное';
        }
    }

    async toggleFavorite(button) {
        const productId = parseInt(button.dataset.productId);
        const isCurrentlyFavorite = this.favoritesMap.get(productId) || false;

        try {
            if (isCurrentlyFavorite) {
                await this.removeFromFavorites(productId);
                this.favoritesMap.set(productId, false);
                this.showNotification('Товар удален из избранного', 'success');
            } else {
                await this.addToFavorites(productId);
                this.favoritesMap.set(productId, true);
                this.showNotification('Товар добавлен в избранное!', 'success');
            }
            this.updateButtonState(button);
        } catch (error) {
            this.showNotification(error.message, 'error');
        }
    }

    async addToFavorites(productId) {
        const response = await fetch(`/api/favorites/${productId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [this.csrfHeader]: this.csrfToken
            }
        });

        if (response.status === 401) {
            this.showLoginModal();
            throw new Error('Требуется авторизация');
        }

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Ошибка при добавлении в избранное');
        }

        return await response.json();
    }

    async removeFromFavorites(productId) {
        const response = await fetch(`/api/favorites/${productId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                [this.csrfHeader]: this.csrfToken
            }
        });

        if (response.status === 401) {
            this.showLoginModal();
            throw new Error('Требуется авторизация');
        }

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Ошибка при удалении из избранного');
        }

        return await response.json();
    }

    showNotification(message, type = 'info') {
        // Удаляем предыдущие уведомления
        const existingToasts = document.querySelectorAll('.notification-toast');
        existingToasts.forEach(toast => toast.remove());

        const toast = document.createElement('div');
        toast.className = `notification-toast alert alert-${type} alert-dismissible fade show`;
        toast.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        toast.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.body.appendChild(toast);

        setTimeout(() => {
            if (toast.parentNode) {
                const bsAlert = new bootstrap.Alert(toast);
                bsAlert.close();
            }
        }, 3000);
    }

    showLoginModal() {
        const loginModal = new bootstrap.Modal(document.getElementById('loginModal'));
        loginModal.show();
    }
}

// Глобальная инициализация
const favoritesManager = new FavoritesManager();