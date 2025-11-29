// ================================
// statistics-charts.js — С РЕАЛЬНЫМИ ДАННЫМИ
// ================================

const COLOR_PALETTE = [
    '#656d4a', '#7f4f24', '#414833', '#333d29', '#a68a64', '#582f0e'
];

// Глобальные настройки Chart.js для адаптивности
Chart.defaults.font.family = "Inter, sans-serif";
Chart.defaults.responsive = true;
Chart.defaults.maintainAspectRatio = false;
Chart.defaults.plugins.legend.labels.usePointStyle = true;
Chart.defaults.plugins.legend.labels.pointStyle = "circle";
Chart.defaults.plugins.legend.labels.boxWidth = 10;
Chart.defaults.elements.line.borderWidth = 2;
Chart.defaults.elements.point.radius = 3;
Chart.defaults.elements.bar.borderRadius = 4;

// Адаптивные настройки для разных брейкпоинтов
function getResponsiveConfig() {
    const width = window.innerWidth;

    if (width < 768) { // Mobile
        return {
            fontSize: 12,
            pointRadius: 2,
            borderWidth: 1,
            legendPosition: 'bottom',
            barThickness: 'flex',
            categoryPercentage: 0.6
        };
    } else if (width < 1200) { // Tablet
        return {
            fontSize: 13,
            pointRadius: 3,
            borderWidth: 2,
            legendPosition: 'top',
            barThickness: 'flex',
            categoryPercentage: 0.7
        };
    } else { // Desktop
        return {
            fontSize: 14,
            pointRadius: 4,
            borderWidth: 3,
            legendPosition: 'top',
            barThickness: 'flex',
            categoryPercentage: 0.8
        };
    }
}

// Функция создания градиента
function createGradient(ctx, chartArea, color) {
    if (!chartArea) return color + '40';

    const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
    gradient.addColorStop(0, color + '40');
    gradient.addColorStop(1, color + '10');
    return gradient;
}

// Основная функция инициализации графиков с реальными данными
function initializeChartsWithRealData(analyticsData) {
    try {
        const config = getResponsiveConfig();

        console.log('Initializing charts with real data:', analyticsData);

        // Динамика продаж (Line Chart) - реальные данные dailyOrders
        initializeSalesChart(analyticsData, config);

        // Распределение по категориям (Doughnut Chart) - реальные данные revenueByCategory
        initializeCategoryChart(analyticsData, config);

        // Топ товаров (Bar Chart) - реальные данные topSellingProducts
        initializeProductsChart(analyticsData, config);

        // Эффективность каналов (Polar Area Chart) - статические данные
        initializeChannelChart(analyticsData, config);

        console.log('All charts initialized with real data!');

    } catch (error) {
        console.error('Error initializing charts with real data:', error);
        // Резервная инициализация с демо-данными
        initializeWithDemoData();
    }
}

// Инициализация графика продаж с реальными данными dailyOrders
function initializeSalesChart(analyticsData, config) {
    const salesCtx = document.getElementById("salesChart");
    if (!salesCtx) return;

    salesCtx.style.width = '100%';
    salesCtx.style.height = '250px';

    // Получаем реальные данные dailyOrders
    const dailyOrders = analyticsData.dailyOrders || {};
    const labels = Object.keys(dailyOrders).map(date => {
        // Форматируем дату для отображения
        const [year, month, day] = date.split('-');
        return `${day}.${month}`;
    });

    const data = Object.values(dailyOrders);

    // Если данных нет, используем демо-данные
    const finalLabels = labels.length > 0 ? labels : ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
    const finalData = data.length > 0 ? data : [12, 19, 8, 15, 12, 18, 10];

    new Chart(salesCtx, {
        type: "line",
        data: {
            labels: finalLabels,
            datasets: [{
                label: "Количество заказов",
                data: finalData,
                borderColor: COLOR_PALETTE[0],
                backgroundColor: function(context) {
                    const chart = context.chart;
                    const {ctx, chartArea} = chart;
                    return createGradient(ctx, chartArea, COLOR_PALETTE[0]);
                },
                fill: true,
                tension: 0.4,
                borderWidth: config.borderWidth,
                pointRadius: config.pointRadius,
                pointHoverRadius: config.pointRadius + 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: config.legendPosition,
                    labels: {
                        font: { size: config.fontSize },
                        usePointStyle: true
                    }
                },
                tooltip: {
                    bodyFont: { size: config.fontSize - 1 },
                    titleFont: { size: config.fontSize },
                    callbacks: {
                        label: function(context) {
                            return `Заказы: ${context.parsed.y}`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    grid: { color: "rgba(0,0,0,0.05)", drawBorder: false },
                    ticks: { font: { size: config.fontSize - 1 } }
                },
                y: {
                    grid: { color: "rgba(0,0,0,0.05)", drawBorder: false },
                    ticks: { font: { size: config.fontSize - 1 } },
                    beginAtZero: true
                }
            },
            interaction: { intersect: false, mode: 'index' }
        }
    });
}

// Инициализация графика категорий с реальными данными revenueByCategory
function initializeCategoryChart(analyticsData, config) {
    const categoryCtx = document.getElementById("categoryChart");
    if (!categoryCtx) return;

    categoryCtx.style.width = '100%';
    categoryCtx.style.height = '250px';

    // Получаем реальные данные revenueByCategory
    const revenueByCategory = analyticsData.revenueByCategory || {};
    const labels = Object.keys(revenueByCategory);
    const data = Object.values(revenueByCategory).map(val =>
        typeof val === 'number' ? val : parseFloat(val) || 0
    );

    // Если данных нет, используем демо-данные
    const finalLabels = labels.length > 0 ? labels : ['Одежда', 'Обувь', 'Аксессуары'];
    const finalData = data.length > 0 ? data : [150000, 80000, 45000];

    new Chart(categoryCtx, {
        type: "doughnut",
        data: {
            labels: finalLabels,
            datasets: [{
                data: finalData,
                backgroundColor: COLOR_PALETTE,
                borderColor: "#ffffff",
                borderWidth: window.innerWidth < 768 ? 1 : 2,
                hoverOffset: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: window.innerWidth < 768 ? 'bottom' : 'right',
                    labels: {
                        padding: 15,
                        usePointStyle: true,
                        font: { size: config.fontSize - 1 },
                        boxWidth: 8
                    }
                },
                tooltip: {
                    bodyFont: { size: config.fontSize - 1 },
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.raw || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = Math.round((value / total) * 100);
                            return `${label}: ${value.toLocaleString('ru-RU')} ₽ (${percentage}%)`;
                        }
                    }
                }
            },
            cutout: window.innerWidth < 768 ? '50%' : '60%'
        }
    });
}

// Инициализация графика топ товаров с реальными данными topSellingProducts
function initializeProductsChart(analyticsData, config) {
    const growthCtx = document.getElementById("growthChart");
    if (!growthCtx) return;

    growthCtx.style.width = '100%';
    growthCtx.style.height = '250px';

    // Получаем реальные данные topSellingProducts
    const topProducts = analyticsData.topSellingProducts || {};
    const labels = Object.keys(topProducts);
    const data = Object.values(topProducts);

    // Ограничиваем до 5 топовых товаров для лучшего отображения
    const limitedLabels = labels.slice(0, 5);
    const limitedData = data.slice(0, 5);

    // Если данных нет, используем демо-данные
    const finalLabels = limitedLabels.length > 0 ? limitedLabels : ['Футболка', 'Джинсы', 'Кроссовки'];
    const finalData = limitedData.length > 0 ? limitedData : [25, 18, 12];

    new Chart(growthCtx, {
        type: "bar",
        data: {
            labels: finalLabels,
            datasets: [{
                label: "Количество продаж",
                data: finalData,
                backgroundColor: COLOR_PALETTE,
                borderWidth: 0,
                barPercentage: 0.6,
                categoryPercentage: config.categoryPercentage
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    bodyFont: { size: config.fontSize - 1 },
                    callbacks: {
                        label: function(context) {
                            return `Продажи: ${context.parsed.y} шт.`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    grid: { display: false, drawBorder: false },
                    ticks: {
                        font: { size: config.fontSize - 1 },
                        maxRotation: 45,
                        minRotation: 45
                    }
                },
                y: {
                    grid: { color: "rgba(0,0,0,0.05)", drawBorder: false },
                    ticks: { font: { size: config.fontSize - 1 } },
                    beginAtZero: true
                }
            }
        }
    });
}

// Инициализация графика каналов (статические данные)
function initializeChannelChart(analyticsData, config) {
    const channelCtx = document.getElementById("channelChart");
    if (!channelCtx) return;

    channelCtx.style.width = '100%';
    channelCtx.style.height = '250px';

    // Статические данные для каналов продаж
    const channelData = {
        labels: ['Сайт', 'Мобильное приложение', 'Соцсети', 'Email', 'Оффлайн'],
        data: [45, 25, 15, 10, 5]
    };

    new Chart(channelCtx, {
        type: "polarArea",
        data: {
            labels: channelData.labels,
            datasets: [{
                data: channelData.data,
                backgroundColor: COLOR_PALETTE,
                borderWidth: window.innerWidth < 768 ? 1 : 2,
                borderColor: '#ffffff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 10,
                        usePointStyle: true,
                        font: { size: config.fontSize - 1 },
                        boxWidth: 8
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return `${context.label}: ${context.parsed.r}%`;
                        }
                    }
                }
            },
            scales: {
                r: {
                    grid: { color: "rgba(0,0,0,0.1)" },
                    ticks: {
                        display: false,
                        backdropColor: 'transparent'
                    }
                }
            }
        }
    });
}

// Резервная инициализация с демо-данными
function initializeWithDemoData() {
    console.log('Using demo data for charts');

    const demoData = {
        dailyOrders: {
            '2024-01-01': 12,
            '2024-01-02': 19,
            '2024-01-03': 8,
            '2024-01-04': 15,
            '2024-01-05': 12,
            '2024-01-06': 18,
            '2024-01-07': 10
        },
        revenueByCategory: {
            'Одежда': 150000,
            'Обувь': 80000,
            'Аксессуары': 45000
        },
        topSellingProducts: {
            'Футболка хлопковая': 25,
            'Джинсы классические': 18,
            'Кроссовки спортивные': 12,
            'Рюкзак городской': 8
        }
    };

    initializeChartsWithRealData(demoData);
}

// Обработчик изменения размера окна
let resizeTimeout;
function handleResize() {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(() => {
        const analyticsData = getAnalyticsData();
        destroyAllCharts();
        setTimeout(() => initializeChartsWithRealData(analyticsData), 100);
    }, 250);
}

// Уничтожение всех графиков
function destroyAllCharts() {
    Object.values(Chart.instances).forEach(chart => chart.destroy());
}

// Получение реальных данных аналитики из Thymeleaf
function getAnalyticsData() {
    try {
        // Получаем объект analytics из Thymeleaf
        const analytics = window.analyticsData || {};

        return {
            dailyOrders: analytics.dailyOrders || {},
            revenueByCategory: analytics.revenueByCategory || {},
            topSellingProducts: analytics.topSellingProducts || {},
            dailyLabels: analytics.dailyLabels || [],
            dailyData: analytics.dailyData || []
        };
    } catch (error) {
        console.error('Error getting analytics data:', error);
        return {};
    }
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    // Передаем данные из Thymeleaf в JavaScript
    window.analyticsData = {
        dailyOrders: /*[[${analytics?.dailyOrders}]]*/ {},
        revenueByCategory: /*[[${analytics?.revenueByCategory}]]*/ {},
        topSellingProducts: /*[[${analytics?.topSellingProducts}]]*/ {},
        dailyLabels: /*[[${dailyLabels}]]*/ [],
        dailyData: /*[[${dailyData}]]*/ []
    };

    console.log('Thymeleaf analytics data:', window.analyticsData);

    // Ждем полной загрузки DOM
    setTimeout(() => {
        const analyticsData = getAnalyticsData();
        initializeChartsWithRealData(analyticsData);
    }, 500);

    // Добавляем обработчик изменения размера
    window.addEventListener('resize', handleResize);
});

// Очистка при разгрузке страницы
window.addEventListener('beforeunload', function() {
    window.removeEventListener('resize', handleResize);
    destroyAllCharts();
});