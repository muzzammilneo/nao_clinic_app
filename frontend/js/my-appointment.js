document.addEventListener('DOMContentLoaded', () => {
  const lookupForm = document.getElementById('lookup-form');
  const phoneInput = document.getElementById('lookupPhone');
  const idInput = document.getElementById('lookupId');
  const resultCard = document.getElementById('appointment-result-card');
  const emptyState = document.getElementById('lookup-empty-state');

  let currentAppointment = null;

  // Auto-populate from URL if present
  const params = new URLSearchParams(window.location.search);
  if (params.get('phone') && params.get('id')) {
    phoneInput.value = params.get('phone');
    idInput.value = params.get('id');
    performLookup(params.get('phone'), params.get('id'));
  }

  if (lookupForm) {
    lookupForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const phone = phoneInput.value.trim();
      const id = idInput.value.trim();
      if (!phone || !id) {
        window.ClinicAPI.showToast('Please provide both phone number and appointment ID.', 'error');
        return;
      }
      performLookup(phone, id);
    });
  }

  async function performLookup(phone, id) {
    try {
      const appt = await window.ClinicAPI.lookupAppointment(phone, id);
      currentAppointment = appt;
      renderResult(appt);
      window.ClinicAPI.showToast('Appointment found!', 'success');
    } catch (err) {
      currentAppointment = null;
      if (resultCard) resultCard.style.display = 'none';
      if (emptyState) emptyState.style.display = 'block';
      window.ClinicAPI.showToast(err.message || 'No matching appointment found.', 'error');
    }
  }

  function renderResult(appt) {
    if (!resultCard) return;
    if (emptyState) emptyState.style.display = 'none';
    resultCard.style.display = 'block';

    const statusBadge = getStatusBadge(appt.status);

    document.getElementById('result-id').textContent = `#${appt.id}`;
    document.getElementById('result-status-badge').innerHTML = statusBadge;
    document.getElementById('result-patient-name').textContent = appt.patientName;
    document.getElementById('result-patient-phone').textContent = appt.patientPhone;
    document.getElementById('result-doctor-name').textContent = appt.doctorName;
    document.getElementById('result-doctor-spec').textContent = appt.doctorSpecialization;
    document.getElementById('result-date').textContent = appt.appointmentDate;
    document.getElementById('result-time').textContent = appt.appointmentTime.slice(0, 5);

    const cancelBtn = document.getElementById('cancel-appointment-btn');
    if (cancelBtn) {
      if (appt.status === 'CANCELLED' || appt.status === 'REJECTED') {
        cancelBtn.style.display = 'none';
      } else {
        cancelBtn.style.display = 'inline-flex';
        cancelBtn.onclick = handleCancel;
      }
    }
  }

  async function handleCancel() {
    if (!currentAppointment) return;

    const confirmed = confirm('Are you sure you want to cancel this appointment? This action cannot be undone.');
    if (!confirmed) return;

    try {
      const updated = await window.ClinicAPI.cancelAppointment(currentAppointment.id, currentAppointment.patientPhone);
      currentAppointment = updated;
      renderResult(updated);
      window.ClinicAPI.showToast('Your appointment has been successfully cancelled.', 'success');
    } catch (err) {
      window.ClinicAPI.showToast(err.message || 'Failed to cancel appointment', 'error');
    }
  }

  function getStatusBadge(status) {
    switch (status) {
      case 'APPROVED':
        return '<span class="badge badge-approved">Approved</span>';
      case 'PENDING':
        return '<span class="badge badge-pending">Pending Review</span>';
      case 'REJECTED':
        return '<span class="badge badge-rejected">Rejected</span>';
      case 'CANCELLED':
        return '<span class="badge badge-cancelled">Cancelled</span>';
      default:
        return `<span class="badge">${status}</span>`;
    }
  }
});
