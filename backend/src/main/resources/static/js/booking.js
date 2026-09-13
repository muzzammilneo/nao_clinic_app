document.addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('booking-form');
  const doctorSelect = document.getElementById('doctorSelect');
  const dateInput = document.getElementById('appointmentDate');
  const timeSelect = document.getElementById('appointmentTime');
  const doctorInfoBox = document.getElementById('doctor-availability-info');
  const confirmationModal = document.getElementById('confirmation-modal');

  let doctors = [];

  // Set minimum date to today
  const today = new Date().toISOString().split('T')[0];
  if (dateInput) {
    dateInput.min = today;
  }

  // Populate time slots
  const standardTimes = [
    '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
    '14:00', '14:30', '15:00', '15:30', '16:00', '16:30'
  ];

  if (timeSelect) {
    timeSelect.innerHTML = '<option value="">-- Select Time Slot --</option>' +
      standardTimes.map(t => `<option value="${t}">${formatTimeLabel(t)}</option>`).join('');
  }

  function formatTimeLabel(timeStr) {
    const [h, m] = timeStr.split(':').map(Number);
    const ampm = h >= 12 ? 'PM' : 'AM';
    const displayH = h % 12 || 12;
    return `${displayH}:${m === 0 ? '00' : m} ${ampm}`;
  }

  // Fetch doctors and populate dropdown
  try {
    doctors = await window.ClinicAPI.getDoctors();
    if (doctorSelect) {
      doctorSelect.innerHTML = '<option value="">-- Choose a Doctor --</option>' +
        doctors.map(d => `<option value="${d.id}">${escapeHtml(d.name)} (${escapeHtml(d.specialization)})</option>`).join('');

      // Check URL for preselected doctor
      const urlParams = new URLSearchParams(window.location.search);
      const preselectedId = urlParams.get('doctorId');
      if (preselectedId) {
        doctorSelect.value = preselectedId;
        updateDoctorAvailability();
      }
    }
  } catch (err) {
    window.ClinicAPI.showToast('Could not load doctors from server. Please verify backend is running.', 'error');
  }

  if (doctorSelect) {
    doctorSelect.addEventListener('change', updateDoctorAvailability);
  }

  if (dateInput) {
    dateInput.addEventListener('change', validateSelectedDateDay);
  }

  function updateDoctorAvailability() {
    const selectedId = Number(doctorSelect.value);
    const doctor = doctors.find(d => d.id === selectedId);

    if (!doctor || !doctorInfoBox) {
      if (doctorInfoBox) doctorInfoBox.style.display = 'none';
      return;
    }

    doctorInfoBox.style.display = 'block';
    doctorInfoBox.innerHTML = `
      <div style="background: var(--primary-light); color: var(--primary); padding: 0.75rem 1rem; border-radius: var(--radius-md); font-size: 0.875rem;">
        <strong>Available Days:</strong> ${escapeHtml(doctor.availableDays || 'Everyday')}
      </div>
    `;

    validateSelectedDateDay();
  }

  function validateSelectedDateDay() {
    if (!dateInput.value || !doctorSelect.value) return;

    const selectedId = Number(doctorSelect.value);
    const doctor = doctors.find(d => d.id === selectedId);
    if (!doctor || !doctor.availableDays) return;

    const [year, month, day] = dateInput.value.split('-').map(Number);
    const chosenDate = new Date(year, month - 1, day);
    const dayName = chosenDate.toLocaleDateString('en-US', { weekday: 'long' });

    const available = doctor.availableDays.split(',').map(s => s.trim().toLowerCase());
    const match = available.some(d => dayName.toLowerCase().includes(d));

    if (!match) {
      window.ClinicAPI.showToast(`Notice: ${doctor.name} is scheduled for ${doctor.availableDays}. ${dayName} might not be available.`, 'error');
    }
  }

  // Submit booking form
  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();

      const patientName = document.getElementById('patientName').value.trim();
      const patientPhone = document.getElementById('patientPhone').value.trim();
      const doctorId = Number(doctorSelect.value);
      const appointmentDate = dateInput.value;
      const appointmentTime = timeSelect.value;

      if (!patientName || !patientPhone || !doctorId || !appointmentDate || !appointmentTime) {
        window.ClinicAPI.showToast('Please fill in all required fields.', 'error');
        return;
      }

      const submitBtn = form.querySelector('button[type="submit"]');
      const originalText = submitBtn.innerHTML;
      submitBtn.disabled = true;
      submitBtn.innerHTML = 'Scheduling...';

      try {
        const result = await window.ClinicAPI.bookAppointment({
          patientName,
          patientPhone,
          doctorId,
          appointmentDate,
          appointmentTime: appointmentTime.length === 5 ? `${appointmentTime}:00` : appointmentTime
        });

        // Show confirmation modal
        showConfirmationModal(result);
        form.reset();
        if (doctorInfoBox) doctorInfoBox.style.display = 'none';
      } catch (err) {
        window.ClinicAPI.showToast(err.message || 'Failed to book appointment', 'error');
      } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
      }
    });
  }

  function showConfirmationModal(appt) {
    if (!confirmationModal) return;

    document.getElementById('confirm-id').textContent = `#${appt.id}`;
    document.getElementById('confirm-name').textContent = appt.patientName;
    document.getElementById('confirm-phone').textContent = appt.patientPhone;
    document.getElementById('confirm-doctor').textContent = `${appt.doctorName} (${appt.doctorSpecialization})`;
    document.getElementById('confirm-datetime').textContent = `${appt.appointmentDate} at ${appt.appointmentTime.slice(0, 5)}`;
    
    const lookupBtn = document.getElementById('confirm-lookup-link');
    if (lookupBtn) {
      lookupBtn.href = `my-appointment.html?phone=${encodeURIComponent(appt.patientPhone)}&id=${appt.id}`;
    }

    confirmationModal.classList.add('open');
  }

  const closeModalBtn = document.getElementById('close-modal-btn');
  if (closeModalBtn) {
    closeModalBtn.addEventListener('click', () => {
      confirmationModal.classList.remove('open');
    });
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
});
