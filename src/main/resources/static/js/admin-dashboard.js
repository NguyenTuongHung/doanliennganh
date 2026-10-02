import { requireDashboardRole } from './features/dashboard-auth.js';

const $ = selector => document.querySelector(selector);
const dashboard = await requireDashboardRole(['SUPER_ADMIN']);
const html = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
$('#admin-email').textContent = dashboard.user.email;

async function loadAdminData() {
  $('#admin-message').textContent = 'Đang tải dữ liệu quản trị…';
  const [overview, venues] = await Promise.all([
    dashboard.request('/api/v1/admin/overview'), dashboard.request('/api/v1/admin/venues/pending')
  ]);
  $('#metric-users').textContent = overview.users;
  $('#metric-venues').textContent = overview.venues;
  $('#metric-pending').textContent = overview.pendingVenues;
  $('#metric-bookings').textContent = overview.activeBookings;
  $('#pending-venue-list').innerHTML = venues.length ? venues.map(venue => `
    <article class="pending-venue-row">
      <div><h3>${html(venue.name)}</h3><p>${html(venue.address)} · ${html(venue.city)}</p>
        <p>Chủ sân: ${html(venue.ownerName)} · ${html(venue.ownerEmail)}</p></div>
      <div class="venue-actions"><button class="approve-venue" data-venue-id="${venue.id}" data-decision="ACTIVE">Duyệt</button>
        <button class="reject-venue" data-venue-id="${venue.id}" data-decision="REJECTED">Từ chối</button></div>
    </article>`).join('') : '<div class="dashboard-empty">Không có hồ sơ nào đang chờ duyệt.</div>';
  $('#admin-message').textContent = '';
}

$('#pending-venue-list').addEventListener('click', async event => {
  const button = event.target.closest('[data-decision]');
  if (!button) return;
  button.disabled = true;
  try {
    await dashboard.request(`/api/v1/admin/venues/${button.dataset.venueId}/decision`, {
      method: 'PUT', body: JSON.stringify({ status: button.dataset.decision })
    });
    $('#admin-message').textContent = button.dataset.decision === 'ACTIVE' ? 'Đã duyệt cơ sở.' : 'Đã từ chối cơ sở.';
    await loadAdminData();
  } catch (error) {
    $('#admin-message').textContent = error.message;
    button.disabled = false;
  }
});

$('#refresh-admin').addEventListener('click', () => loadAdminData().catch(error => { $('#admin-message').textContent = error.message; }));
loadAdminData().catch(error => { $('#admin-message').textContent = `Không tải được dữ liệu: ${error.message}`; });
