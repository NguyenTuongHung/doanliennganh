import { requireDashboardRole } from './features/dashboard-auth.js';

const $ = selector => document.querySelector(selector);
const dashboard = await requireDashboardRole(['CUSTOMER']);
const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
const localDateTime = value => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
const html = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

function showProfile(profile) {
  $('#welcome-name').textContent = profile.fullName || profile.email;
  $('#profile-role').textContent = profile.roles.join(', ');
  const form = $('#profile-form');
  form.elements.fullName.value = profile.fullName || '';
  form.elements.email.value = profile.email || '';
  form.elements.phone.value = profile.phone || '';
}

async function loadProfile() {
  showProfile(await dashboard.request('/api/v1/auth/me'));
}

async function loadBookings() {
  const bookings = await dashboard.request('/api/v1/bookings/mine');
  if (!bookings.length) {
    $('#booking-history').innerHTML = '<div class="dashboard-empty">Chưa có lịch đặt sân. <a href="/#tim-san">Tìm sân để bắt đầu →</a></div>';
    return;
  }
  $('#booking-history').innerHTML = bookings.map(booking => `
    <article class="booking-row">
      <div><h3>${html(booking.venueName)} · ${html(booking.courtName)}</h3>
        <p>${localDateTime(booking.startsAt)} – ${new Intl.DateTimeFormat('vi-VN', { timeStyle: 'short' }).format(new Date(booking.endsAt))}</p>
        <p>Mã ${html(booking.bookingCode)}</p></div>
      <aside><b>${money.format(Number(booking.totalAmount))}</b>
        <span class="booking-status status-${html(booking.status)}">${html(booking.status)}</span></aside>
    </article>`).join('');
}

$('#profile-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const message = $('#profile-message');
  const submit = form.querySelector('[type="submit"]');
  submit.disabled = true;
  message.textContent = 'Đang lưu…';
  try {
    const profile = await dashboard.request('/api/v1/auth/me', {
      method: 'PUT', body: JSON.stringify({ fullName: form.elements.fullName.value, phone: form.elements.phone.value })
    });
    showProfile(profile);
    message.textContent = 'Đã lưu hồ sơ.';
  } catch (error) {
    message.textContent = error.message;
  } finally {
    submit.disabled = false;
  }
});

$('#refresh-bookings').addEventListener('click', () => loadBookings().catch(error => {
  $('#booking-history').innerHTML = `<div class="dashboard-empty">${html(error.message)}</div>`;
}));

try {
  await Promise.all([loadProfile(), loadBookings()]);
} catch (error) {
  $('#booking-history').innerHTML = `<div class="dashboard-empty">Không tải được dữ liệu: ${html(error.message)}</div>`;
}
