function initAdminDashboard() {
  // DOM element references grabbed at the top
  const adminUsernameEl = document.getElementById('admin-username');
  const logoutBtn = document.getElementById('logout-btn');
  const filterDoctorSelect = document.getElementById('filter-doctor');
  const filterDateInput = document.getElementById('filter-date');
  const resetFiltersBtn = document.getElementById('reset-filters-btn');
  const appointmentsTbody = document.getElementById('appointments-tbody');
  const doctorsTbody = document.getElementById('doctors-tbody');

  // Reschedule modal elements
  const rescheduleModal = document.getElementById('reschedule-modal');
  const rescheduleForm = document.getElementById('reschedule-form');
  const rescheduleDateInput = document.getElementById('reschedule-date');
  const rescheduleTimeInput = document.getElementById('reschedule-time');

  // Doctor modal elements
  const doctorModal = document.getElementById('doctor-modal');
  const doctorForm = document.getElementById('doctor-form');
  const doctorModalTitle = document.getElementById('doctor-modal-title');
  const docNameInput = document.getElementById('doc-name');
  const docSpecInput = document.getElementById('doc-spec');
  const docDaysInput = document.getElementById('doc-days');
  const addDoctorBtn = document.getElementById('add-doctor-btn');

  // State
  let doctorsList = [];
  let currentRescheduleId = null;
  let currentEditingDoctorId = null;

  // Populate reschedule standard time slots
  const standardTimes = [
    '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
    '14:00', '14:30', '15:00', '15:30', '16:00', '16:30'
  ];
  if (rescheduleTimeInput) {
    rescheduleTimeInput.innerHTML = standardTimes.map(t => `<option value="${t}">${t}</option>`).join('');
  }

  // Setup tabs
  const tabButtons = document.querySelectorAll('.tab-btn');
  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      tabButtons.forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      const targetId = btn.getAttribute('data-tab');
      const targetContent = document.getElementById(targetId);
      if (targetContent) targetContent.classList.add('active');
    });
  });

  // Setup filter listeners
  if (filterDoctorSelect) {
    filterDoctorSelect.addEventListener('change', loadAppointments);
  }
  if (filterDateInput) {
    filterDateInput.addEventListener('change', loadAppointments);
  }
  if (resetFiltersBtn) {
    resetFiltersBtn.addEventListener('click', () => {
      if (filterDoctorSelect) filterDoctorSelect.value = '';
      if (filterDateInput) filterDateInput.value = '';
      loadAppointments();
    });
  }

  // Setup logout
  if (logoutBtn) {
    logoutBtn.addEventListener('click', async () => {
      try {
        await window.ClinicAPI.adminLogout();
      } finally {
        window.location.href = 'login.html';
      }
    });
  }

  // Modal Cancel / Close Buttons
  document.querySelectorAll('.modal-close, .modal-cancel').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.modal-backdrop').forEach(m => m.classList.remove('open'));
    });
  });

  // Check admin session and load dashboard data
  (async function verifyAndLoad() {
    try {
      const session = await window.ClinicAPI.checkAdminSession();
      if (!session || !session.authenticated) {
        window.location.href = 'login.html';
        return;
      }
      if (adminUsernameEl) {
        adminUsernameEl.textContent = session.username || 'Admin';
      }
    } catch (err) {
      console.warn('Session verification failed, redirecting to login:', err);
      window.location.href = 'login.html';
      return;
    }

    // Load data
    await loadDoctors();
    await loadAppointments();
  })();

  // Functions
  async function loadDoctors() {
    try {
      doctorsList = await window.ClinicAPI.getDoctors();
      renderDoctorsTable(doctorsList);
      populateDoctorFilters(doctorsList);
    } catch (err) {
      console.error('Failed to load doctors:', err);
      if (doctorsTbody) {
        doctorsTbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--danger); padding: 2rem;">Failed to load doctors roster.</td></tr>`;
      }
    }
  }

  function populateDoctorFilters(docs) {
    if (!filterDoctorSelect) return;
    const currentVal = filterDoctorSelect.value;
    filterDoctorSelect.innerHTML = '<option value="">All Doctors</option>' +
      docs.map(d => `<option value="${d.id}">${escapeHtml(d.name)}</option>`).join('');
    filterDoctorSelect.value = currentVal;
  }

  async function loadAppointments() {
    if (!appointmentsTbody) return;

    const doctorId = filterDoctorSelect && filterDoctorSelect.value ? filterDoctorSelect.value : null;
    const date = filterDateInput && filterDateInput.value ? filterDateInput.value : null;

    appointmentsTbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 2rem;">Loading appointments...</td></tr>`;

    try {
      const appointments = await window.ClinicAPI.getAdminAppointments(doctorId, date);
      renderAppointmentsTable(appointments);
    } catch (err) {
      console.error('Failed to load appointments:', err);
      appointmentsTbody.innerHTML = `
        <tr>
          <td colspan="7" style="text-align: center; color: var(--danger); padding: 2rem;">
            Failed to load appointments: ${escapeHtml(err.message || 'Unknown error')}
          </td>
        </tr>
      `;
      window.ClinicAPI.showToast(err.message || 'Error loading appointments', 'error');
    }
  }

  function renderAppointmentsTable(list) {
    if (!appointmentsTbody) return;

    if (!list || list.length === 0) {
      appointmentsTbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-muted);">No appointments found.</td></tr>`;
      return;
    }

    appointmentsTbody.innerHTML = list.map(a => {
      const statusBadge = getStatusBadge(a.status);
      const isPending = a.status === 'PENDING';
      const isApproved = a.status === 'APPROVED';
      const timeDisplay = a.appointmentTime ? String(a.appointmentTime).slice(0, 5) : '--:--';
      const dateDisplay = a.appointmentDate || '--';

      return `
        <tr>
          <td><strong>#${a.id}</strong></td>
          <td>
            <div style="font-weight: 600;">${escapeHtml(a.patientName)}</div>
            <div style="font-size: 0.82rem; color: var(--text-muted);">${escapeHtml(a.patientPhone)}</div>
          </td>
          <td>
            <div>${escapeHtml(a.doctorName || 'Unknown Doctor')}</div>
            <div style="font-size: 0.8rem; color: var(--primary);">${escapeHtml(a.doctorSpecialization || '')}</div>
          </td>
          <td>${dateDisplay}</td>
          <td>${timeDisplay}</td>
          <td>${statusBadge}</td>
          <td>
            <div style="display: flex; gap: 0.4rem; flex-wrap: wrap;">
              ${isPending ? `
                <button class="btn btn-sm btn-success" onclick="window.updateStatus(${a.id}, 'APPROVED')">Approve</button>
                <button class="btn btn-sm btn-danger" onclick="window.updateStatus(${a.id}, 'REJECTED')">Reject</button>
              ` : ''}
              ${(isPending || isApproved) ? `
                <button class="btn btn-sm btn-secondary" onclick="window.openRescheduleModal(${a.id}, '${dateDisplay}', '${timeDisplay}')">Reschedule</button>
              ` : ''}
              ${isApproved ? `
                <button class="btn btn-sm btn-danger" onclick="window.updateStatus(${a.id}, 'CANCELLED')">Cancel</button>
              ` : ''}
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  function renderDoctorsTable(list) {
    if (!doctorsTbody) return;

    if (!list || list.length === 0) {
      doctorsTbody.innerHTML = `<tr><td colspan="5" style="text-align: center; padding: 2rem; color: var(--text-muted);">No doctors added yet.</td></tr>`;
      return;
    }

    doctorsTbody.innerHTML = list.map(d => `
      <tr>
        <td><strong>#${d.id}</strong></td>
        <td style="font-weight: 600;">${escapeHtml(d.name)}</td>
        <td><span class="badge badge-specialty">${escapeHtml(d.specialization)}</span></td>
        <td>${escapeHtml(d.availableDays || 'Everyday')}</td>
        <td>
          <div style="display: flex; gap: 0.5rem;">
            <button class="btn btn-sm btn-secondary" onclick="window.openEditDoctorModal(${d.id})">Edit</button>
            <button class="btn btn-sm btn-danger" onclick="window.deleteDoctor(${d.id})">Remove</button>
          </div>
        </td>
      </tr>
    `).join('');
  }

  // Global window functions for table action buttons
  window.updateStatus = async (id, status) => {
    try {
      await window.ClinicAPI.updateAppointment(id, { status });
      window.ClinicAPI.showToast(`Appointment #${id} updated to ${status}.`, 'success');
      loadAppointments();
    } catch (err) {
      window.ClinicAPI.showToast(err.message || 'Failed to update status', 'error');
    }
  };

  window.openRescheduleModal = (id, curDate, curTime) => {
    currentRescheduleId = id;
    if (rescheduleDateInput) {
      rescheduleDateInput.value = curDate !== '--' ? curDate : '';
      rescheduleDateInput.min = new Date().toISOString().split('T')[0];
    }
    if (rescheduleTimeInput && curTime !== '--:--') {
      rescheduleTimeInput.value = curTime;
    }
    if (rescheduleModal) rescheduleModal.classList.add('open');
  };

  if (rescheduleForm) {
    rescheduleForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (!currentRescheduleId) return;

      const newDate = rescheduleDateInput.value;
      const newTime = rescheduleTimeInput.value;

      try {
        await window.ClinicAPI.updateAppointment(currentRescheduleId, {
          appointmentDate: newDate,
          appointmentTime: newTime.length === 5 ? `${newTime}:00` : newTime
        });
        window.ClinicAPI.showToast('Appointment rescheduled successfully!', 'success');
        rescheduleModal.classList.remove('open');
        loadAppointments();
      } catch (err) {
        window.ClinicAPI.showToast(err.message || 'Failed to reschedule', 'error');
      }
    });
  }

  if (addDoctorBtn) {
    addDoctorBtn.addEventListener('click', () => {
      currentEditingDoctorId = null;
      if (doctorModalTitle) doctorModalTitle.textContent = 'Add New Doctor';
      if (doctorForm) doctorForm.reset();
      if (doctorModal) doctorModal.classList.add('open');
    });
  }

  window.openEditDoctorModal = (id) => {
    const doc = doctorsList.find(d => d.id === id);
    if (!doc) return;
    currentEditingDoctorId = id;
    if (doctorModalTitle) doctorModalTitle.textContent = 'Edit Doctor';
    docNameInput.value = doc.name;
    docSpecInput.value = doc.specialization;
    docDaysInput.value = doc.availableDays || '';
    if (doctorModal) doctorModal.classList.add('open');
  };

  if (doctorForm) {
    doctorForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = docNameInput.value.trim();
      const specialization = docSpecInput.value.trim();
      const availableDays = docDaysInput.value.trim();

      try {
        if (currentEditingDoctorId) {
          await window.ClinicAPI.editDoctor(currentEditingDoctorId, { name, specialization, availableDays });
          window.ClinicAPI.showToast('Doctor updated successfully', 'success');
        } else {
          await window.ClinicAPI.addDoctor({ name, specialization, availableDays });
          window.ClinicAPI.showToast('Doctor added successfully', 'success');
        }
        doctorModal.classList.remove('open');
        await loadDoctors();
      } catch (err) {
        window.ClinicAPI.showToast(err.message || 'Failed to save doctor', 'error');
      }
    });
  }

  window.deleteDoctor = async (id) => {
    const confirmed = confirm('Are you sure you want to remove this doctor? Existing appointments for this doctor will also be deleted.');
    if (!confirmed) return;

    try {
      await window.ClinicAPI.deleteDoctor(id);
      window.ClinicAPI.showToast('Doctor removed successfully', 'success');
      await loadDoctors();
      await loadAppointments();
    } catch (err) {
      window.ClinicAPI.showToast(err.message || 'Failed to remove doctor', 'error');
    }
  };

  function getStatusBadge(status) {
    switch (status) {
      case 'APPROVED':
        return '<span class="badge badge-approved">Approved</span>';
      case 'PENDING':
        return '<span class="badge badge-pending">Pending</span>';
      case 'REJECTED':
        return '<span class="badge badge-rejected">Rejected</span>';
      case 'CANCELLED':
        return '<span class="badge badge-cancelled">Cancelled</span>';
      default:
        return `<span class="badge">${escapeHtml(status)}</span>`;
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
}

// Execute reliably whether DOM is still loading or already loaded
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initAdminDashboard);
} else {
  initAdminDashboard();
}
