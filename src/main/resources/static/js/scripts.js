// Enable Bootstrap tooltips
document.addEventListener('DOMContentLoaded', function() {
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function(tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
});

// Handle form validation
(function() {
    'use strict';
    window.addEventListener('load', function() {
        var forms = document.getElementsByClassName('needs-validation');
        var validation = Array.prototype.filter.call(forms, function(form) {
            form.addEventListener('submit', function(event) {
                if (form.checkValidity() === false) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                form.classList.add('was-validated');
            }, false);
        });
    }, false);
})();

// Handle Check-in and Check-out
function handleTimeRecord(endpoint, form) {
    // Prevent default form submission
    if (form) {
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            
            // Create FormData from the form
            const formData = new FormData(form);
            
            fetch(endpoint, {
                method: 'POST',
                body: formData  // Use FormData instead of manually setting headers
            })
            .then(response => {
                console.log('Response status:', response.status);
                console.log('Response headers:', Object.fromEntries(response.headers.entries()));
                
                // Log response body for more details
                return response.text().then(text => {
                    console.log('Response body:', text);
                    return { response, text };
                });
            })
            .then(({ response, text }) => {
                if (response.ok) {
                    window.location.href = '/dashboard'; // Redirect to home/dashboard
                } else {
                    // Handle error case
                    console.error('Time record failed');
                    console.error('Error details:', text);
                    // Optionally show an error message to the user
                    alert(`Failed to record time. Status: ${response.status}. Details: ${text}`);
                }
            })
            .catch(error => {
                console.error('Fetch Error:', error);
                alert('An error occurred. Please check the console for details.');
            });
        });
    }
}

// Add event listeners for check-in and check-out buttons
document.addEventListener('DOMContentLoaded', function() {
    const checkInForm = document.querySelector('form[action="/employee/timerecord/checkin"]');
    const checkOutForm = document.querySelector('form[action="/employee/timerecord/checkout"]');

    if (checkInForm) {
        handleTimeRecord('/employee/timerecord/checkin', checkInForm);
    }

    if (checkOutForm) {
        handleTimeRecord('/employee/timerecord/checkout', checkOutForm);
    }
});
