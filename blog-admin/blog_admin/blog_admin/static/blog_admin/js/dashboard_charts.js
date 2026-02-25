document.addEventListener('DOMContentLoaded', function() {
    var chartDefaults = {
        responsive: true,
        maintainAspectRatio: false,
    };

    // Posts per Month
    var postsData = JSON.parse(
        document.getElementById('posts-data').textContent
    );
    new Chart(document.getElementById('postsChart'), {
        type: 'bar',
        data: {
            labels: postsData.map(function(d) { return d.month; }),
            datasets: [{
                label: 'Posts',
                data: postsData.map(function(d) { return d.count; }),
                backgroundColor: 'rgba(65, 118, 144, 0.7)',
                borderColor: 'rgba(65, 118, 144, 1)',
                borderWidth: 1,
            }]
        },
        options: Object.assign({}, chartDefaults, {
            scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
        })
    });

    // Comments per Month
    var commentsData = JSON.parse(
        document.getElementById('comments-data').textContent
    );
    new Chart(document.getElementById('commentsChart'), {
        type: 'line',
        data: {
            labels: commentsData.map(function(d) { return d.month; }),
            datasets: [{
                label: 'Comments',
                data: commentsData.map(function(d) { return d.count; }),
                borderColor: 'rgba(39, 174, 96, 1)',
                backgroundColor: 'rgba(39, 174, 96, 0.1)',
                fill: true,
                tension: 0.3,
            }]
        },
        options: Object.assign({}, chartDefaults, {
            scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
        })
    });

    // Top Posts by Views
    var viewsData = JSON.parse(
        document.getElementById('views-data').textContent
    );
    new Chart(document.getElementById('viewsChart'), {
        type: 'bar',
        data: {
            labels: viewsData.map(function(d) {
                return d.name.length > 30 ? d.name.substring(0, 30) + '...' : d.name;
            }),
            datasets: [{
                label: 'Views',
                data: viewsData.map(function(d) { return d.views; }),
                backgroundColor: 'rgba(187, 154, 247, 0.7)',
                borderColor: 'rgba(187, 154, 247, 1)',
                borderWidth: 1,
            }]
        },
        options: Object.assign({}, chartDefaults, {
            indexAxis: 'y',
            scales: { x: { beginAtZero: true } }
        })
    });
});
