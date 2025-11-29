function toggleFavorite(button) {
    const productId = button.getAttribute('data-product-id');
    const isActive = button.classList.contains('active');

    if (isActive) {
        removeFromFavorites(productId, button);
    } else {
        addToFavorites(productId, button);
    }
}

function addToFavorites(productId, button) {
    fetch(`/api/favorites/${productId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
        }
    })
    .then(response => {
        if (response.ok) {
            button.classList.add('active');
            showNotification('Товар добавлен в избранное!', 'success');
        } else if (response.status === 401) {
            showLoginModal('Для добавления в избранное необходимо авторизоваться');
        } else {
            showNotification('Ошибка при добавлении в избранное', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showNotification('Ошибка при добавлении в избранное', 'error');
    });
}

function removeFromFavorites(productId, button) {
    fetch(`/api/favorites/${productId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
        }
    })
    .then(response => {
        if (response.ok) {
            button.classList.remove('active');
            showNotification('Товар удален из избранного', 'info');
        } else {
            showNotification('Ошибка при удалении из избранного', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showNotification('Ошибка при удалении из избранного', 'error');
    });
}

// Функция для корзины
function addToCart(productId) {
    fetch(`/api/cart/add/${productId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
        }
    })
    .then(response => {
        if (response.ok) {
            showNotification('Товар добавлен в корзину!', 'success');
        } else if (response.status === 401) {
            showLoginModal('Для добавления в корзину необходимо авторизоваться');
        } else {
            showNotification('Ошибка при добавлении в корзину', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showNotification('Ошибка при добавлении в корзину', 'error');
    });
}

// Функция быстрого просмотра
function quickView(productId) {
    window.location.href = '/products/' + productId;
}

// Уведомления
function showNotification(message, type = 'info') {
    // Простое уведомление через alert
    alert(message);
}

function showLoginModal(message) {
    document.getElementById('loginModalMessage').textContent = message;
    new bootstrap.Modal(document.getElementById('loginModal')).show();
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    // Загружаем статус избранного для всех товаров
    loadFavoriteStatus();

    // Автоматическое применение фильтров
    const filterInputs = document.querySelectorAll('#filterForm select, #filterForm input[type="text"], #filterForm input[type="number"]');

    // Авто-поиск с задержкой
    const searchInput = document.getElementById('searchInput');
    let searchTimeout;

    if (searchInput) {
        searchInput.addEventListener('input', function() {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                document.getElementById('filterForm').submit();
            }, 500);
        });
    }

    // Автоматическое применение при изменении селектов
    filterInputs.forEach(input => {
        if (input !== searchInput) {
            input.addEventListener('change', function() {
                document.getElementById('filterForm').submit();
            });
        }
    });

    // Показать/скрыть индикатор загрузки
    const form = document.getElementById('filterForm');
    const loadingSpinner = document.getElementById('loadingSpinner');

    if (form && loadingSpinner) {
        form.addEventListener('submit', function() {
            loadingSpinner.style.display = 'block';
        });
    }
});

// Загрузка статуса избранного (опционально - для предзагрузки состояния)
function loadFavoriteStatus() {
    // Здесь можно добавить запрос для получения статуса избранного
    // если нужно предварительно загрузить состояние кнопок
    console.log('Catalog page loaded');
}

// Обработка нажатия Enter в полях ввода
document.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        const activeElement = document.activeElement;
        if (activeElement && (activeElement.type === 'text' || activeElement.type === 'number')) {
            document.getElementById('filterForm').submit();
        }
    }
});