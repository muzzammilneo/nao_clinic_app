/**
 * Centralized API client wrapper for Clinic Website
 */

const API_BASE = (window.location.protocol === 'http:' || window.location.protocol === 'https:')
  ? '/api'
  : 'http://localhost:8080/api';

async function apiRequest(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  const config = {
    ...options,
    headers,
    credentials: 'include' // include session cookies for auth
  };

  try {
    const response = await fetch(url, config);
    let data = null;

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      const text = await response.text();
      data = text ? { message: text } : {};
    }

    if (!response.ok) {
      const errorMessage = data.message || data.error || `HTTP error ${response.status}`;
      const err = new Error(errorMessage);
      err.status = response.status;
      err.details = data.details || null;
      throw err;
    }

    return data;
  } catch (err) {
    console.error(`API Error on [${options.method || 'GET'}] ${endpoint}:`, err);
    throw err;
  }
}

// Toast notification system
function showToast(message, type = 'success') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span>${message}</span>
    <button style="background:none;border:none;cursor:pointer;font-size:1.1rem;margin-left:10px;" onclick="this.parentElement.remove()">&times;</button>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity 0.4s ease';
    setTimeout(() => toast.remove(), 400);
  }, 4000);
}

// Exported API helpers
window.ClinicAPI = {
  // Public APIs
  getDoctors: () => apiRequest('/doctors'),
  
  bookAppointment: (bookingData) => apiRequest('/appointments', {
    method: 'POST',
    body: JSON.stringify(bookingData)
  }),

  lookupAppointment: (phone, id) => 
    apiRequest(`/appointments?phone=${encodeURIComponent(phone)}&id=${encodeURIComponent(id)}`),

  cancelAppointment: (id, phone) => 
    apiRequest(`/appointments/${id}/cancel?phone=${encodeURIComponent(phone)}`, {
      method: 'PUT'
    }),

  // Admin APIs
  adminLogin: (username, password) => apiRequest('/admin/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  }),

  adminLogout: () => apiRequest('/admin/logout', {
    method: 'POST'
  }),

  checkAdminSession: () => apiRequest('/admin/me'),

  getAdminAppointments: (doctorId, date) => {
    const params = new URLSearchParams();
    if (doctorId) params.append('doctorId', doctorId);
    if (date) params.append('date', date);
    const query = params.toString() ? `?${params.toString()}` : '';
    return apiRequest(`/admin/appointments${query}`);
  },

  updateAppointment: (id, updateData) => apiRequest(`/admin/appointments/${id}`, {
    method: 'PUT',
    body: JSON.stringify(updateData)
  }),

  addDoctor: (doctorData) => apiRequest('/admin/doctors', {
    method: 'POST',
    body: JSON.stringify(doctorData)
  }),

  editDoctor: (id, doctorData) => apiRequest(`/admin/doctors/${id}`, {
    method: 'PUT',
    body: JSON.stringify(doctorData)
  }),

  deleteDoctor: (id) => apiRequest(`/admin/doctors/${id}`, {
    method: 'DELETE'
  }),

  showToast
};
