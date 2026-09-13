document.addEventListener('DOMContentLoaded', async () => {
  const doctorsContainer = document.getElementById('doctors-list');
  const searchInput = document.getElementById('doctor-search');
  const specialtyFilter = document.getElementById('specialty-filter');

  let doctors = [];

  try {
    doctors = await window.ClinicAPI.getDoctors();
    populateSpecialtyFilter(doctors);
    renderDoctors(doctors);
  } catch (err) {
    if (doctorsContainer) {
      doctorsContainer.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 3rem; background: white; border-radius: 12px; border: 1px solid var(--border);">
          <p style="color: var(--danger); font-weight: 600; margin-bottom: 0.5rem;">Failed to load doctors</p>
          <p style="color: var(--text-muted); font-size: 0.9rem;">Make sure the backend server is running on port 8080.</p>
        </div>
      `;
    }
  }

  if (searchInput) {
    searchInput.addEventListener('input', filterDoctors);
  }

  if (specialtyFilter) {
    specialtyFilter.addEventListener('change', filterDoctors);
  }

  function populateSpecialtyFilter(docs) {
    if (!specialtyFilter) return;
    const specialties = [...new Set(docs.map(d => d.specialization).filter(Boolean))];
    specialties.forEach(spec => {
      const opt = document.createElement('option');
      opt.value = spec;
      opt.textContent = spec;
      specialtyFilter.appendChild(opt);
    });
  }

  function filterDoctors() {
    const query = searchInput ? searchInput.value.toLowerCase().trim() : '';
    const selectedSpec = specialtyFilter ? specialtyFilter.value : '';

    const filtered = doctors.filter(doc => {
      const matchName = doc.name.toLowerCase().includes(query);
      const matchSpec = !selectedSpec || doc.specialization === selectedSpec;
      return matchName && matchSpec;
    });

    renderDoctors(filtered);
  }

  function renderDoctors(list) {
    if (!doctorsContainer) return;

    if (list.length === 0) {
      doctorsContainer.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 3rem; background: white; border-radius: 12px; border: 1px solid var(--border);">
          <p style="color: var(--text-muted);">No doctors found matching your criteria.</p>
        </div>
      `;
      return;
    }

    doctorsContainer.innerHTML = list.map(doc => {
      const initials = doc.name.replace(/^(Dr\.\s*)/i, '').split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
      const days = doc.availableDays || 'Contact clinic';

      return `
        <div class="doctor-card">
          <div class="doctor-avatar-wrapper">
            <div class="doctor-avatar-icon">${initials || 'MD'}</div>
          </div>
          <div class="doctor-body">
            <h3 class="doctor-name">${escapeHtml(doc.name)}</h3>
            <div class="doctor-spec">${escapeHtml(doc.specialization || 'General Specialist')}</div>
            <div class="doctor-days">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              <span>${escapeHtml(days)}</span>
            </div>
            <div class="doctor-footer">
              <a href="book-appointment.html?doctorId=${doc.id}" class="btn btn-outline-primary" style="width: 100%;">
                Book Appointment
              </a>
            </div>
          </div>
        </div>
      `;
    }).join('');
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
});
