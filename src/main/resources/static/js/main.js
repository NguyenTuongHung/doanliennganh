import { createAuth } from './features/auth.js';

const $ = (selector) => document.querySelector(selector);
const results = $('#booking-results');
const holdDialog = $('#hold-dialog');
const auth = createAuth();
auth.initialize();
const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
let selectedSportId = '';
let selectedSlot = null;

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character]);
}

async function api(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { 'Content-Type': 'application/json', ...(options.headers || {}) } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.detail || data.message || `Yêu cầu không thành công (${response.status})`);
  return data;
}

function localDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function timeLabel(value) {
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(new Date(value));
}

async function loadSports() {
  const sports = await api('/api/v1/sports');
  const icons = { FOOTBALL: '⚽', BADMINTON: '🏸', TENNIS: '🎾', PICKLEBALL: '🏓', BASKETBALL: '🏀', VOLLEYBALL: '🏐' };
  const pills = $('#sport-pills');
  pills.insertAdjacentHTML('beforeend', sports.map(sport =>
    `<button type="button" class="sport-pill" data-sport-id="${sport.id}">${icons[sport.code] || '🏅'} ${escapeHtml(sport.name)}</button>`
  ).join(''));
  pills.addEventListener('click', event => {
    const button = event.target.closest('.sport-pill');
    if (!button) return;
    pills.querySelectorAll('.sport-pill').forEach(pill => pill.classList.remove('active'));
    button.classList.add('active');
    selectedSportId = button.dataset.sportId;
  });
}

function renderVenue(venue, availability) {
  const courts = availability.courts || [];
  const courtCards = courts.length ? courts.map(court => `
    <div class="booking-court">
      <div class="booking-court-title"><b>${escapeHtml(court.name)}</b><span>${escapeHtml(court.sport)}</span></div>
      <div class="booking-slots">${court.slots.map(slot => {
        const state = slot.status.toLowerCase();
        const available = state === 'available' && Number(slot.price) > 0;
        const label = `${timeLabel(slot.startsAt)} – ${timeLabel(slot.endsAt)}`;
        return `<button type="button" class="booking-slot ${available ? 'available' : 'unavailable'}" ${available ?
          `data-venue-id="${venue.id}" data-court-id="${court.courtId}" data-start="${escapeHtml(slot.startsAt)}" data-end="${escapeHtml(slot.endsAt)}" data-price="${slot.price}" data-venue-name="${escapeHtml(venue.name)}" data-court-name="${escapeHtml(court.name)}"` : 'disabled'}>
          ${label}<small>${available ? money.format(Number(slot.price)) : state === 'held' ? 'Đang giữ' : state === 'booked' ? 'Đã đặt' : state === 'past' ? 'Đã qua' : 'Chưa có giá'}</small>
        </button>`;
      }).join('')}</div>
    </div>`).join('') : '<p class="booking-empty">Cơ sở chưa có sân con hoạt động cho bộ môn này.</p>';

  return `<article class="booking-venue"><div class="booking-venue-heading"><div><h3>${escapeHtml(venue.name)}</h3><p>📍 ${escapeHtml(venue.address)} · ${escapeHtml(venue.city)}</p></div><span>${venue.courtCount} sân</span></div>${courtCards}</article>`;
}

async function searchVenues(event) {
  if (event) event.preventDefault();
  const date = $('#play-date').value;
  if (!date) return;
  results.innerHTML = '<div class="booking-message">Đang tìm sân và tải lịch trống…</div>';
  const params = new URLSearchParams();
  const city = $('#search-city').value.trim();
  if (city) params.set('city', city);
  if (selectedSportId) params.set('sportId', selectedSportId);
  try {
    const venues = await api(`/api/v1/venues/search?${params.toString()}`);
    if (!venues.length) {
      results.innerHTML = '<div class="booking-message">Không tìm thấy cơ sở phù hợp. Hãy thử khu vực hoặc bộ môn khác.</div>';
      return;
    }
    const responses = await Promise.all(venues.map(async venue => {
      const availabilityParams = new URLSearchParams({ date });
      if (selectedSportId) availabilityParams.set('sportId', selectedSportId);
      try {
        const availability = await api(`/api/v1/venues/${venue.id}/availability?${availabilityParams}`);
        return renderVenue(venue, availability);
      } catch (error) {
        return `<article class="booking-venue"><div class="booking-venue-heading"><div><h3>${escapeHtml(venue.name)}</h3><p>${escapeHtml(venue.address)} · ${escapeHtml(venue.city)}</p></div></div><p class="booking-empty">${escapeHtml(error.message)}</p></article>`;
      }
    }));
    results.innerHTML = `<div class="booking-results-heading"><b>${venues.length} cơ sở phù hợp</b><span>Chọn giờ trống để giữ sân</span></div>${responses.join('')}`;
  } catch (error) {
    results.innerHTML = `<div class="booking-message booking-error">${escapeHtml(error.message)}. Kiểm tra backend, MySQL và dữ liệu mẫu.</div>`;
  }
}

results.addEventListener('click', event => {
  const button = event.target.closest('.booking-slot.available');
  if (!button) return;
  selectedSlot = {
    venueId: Number(button.dataset.venueId), courtId: Number(button.dataset.courtId),
    startsAt: button.dataset.start, endsAt: button.dataset.end
  };
  $('#hold-summary').textContent = `${button.dataset.venueName} · ${button.dataset.courtName} · ${timeLabel(selectedSlot.startsAt)} – ${timeLabel(selectedSlot.endsAt)} · ${money.format(Number(button.dataset.price))}`;
  $('#hold-result').textContent = '';
  holdDialog.showModal();
});

$('#hold-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (!selectedSlot) return;
  const authorization = auth.authorizationHeader();
  if (!authorization) {
    $('#hold-result').textContent = 'Bạn cần đăng nhập tài khoản khách hàng trước khi giữ sân.';
    return;
  }
  const submit = event.currentTarget.querySelector('[type="submit"]');
  submit.disabled = true;
  $('#hold-result').textContent = 'Đang kiểm tra lịch và giữ sân…';
  try {
    const booking = await api('/api/v1/bookings/hold', {
      method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID(), Authorization: authorization },
      body: JSON.stringify(selectedSlot)
    });
    $('#hold-result').textContent = `Đã giữ sân ${booking.bookingCode} đến ${new Date(booking.holdExpiresAt).toLocaleTimeString('vi-VN')}. Tiền cọc dự kiến: ${money.format(Number(booking.depositAmount))}.`;
    await searchVenues();
  } catch (error) {
    $('#hold-result').textContent = error.message;
  } finally {
    submit.disabled = false;
  }
});

$('.dialog-close').addEventListener('click', () => holdDialog.close());
$('#tim-san').addEventListener('submit', searchVenues);
$('#play-date').value = localDate(new Date());

loadSports().then(() => searchVenues()).catch(error => {
  results.innerHTML = `<div class="booking-message booking-error">Chưa tải được danh mục bộ môn: ${escapeHtml(error.message)}</div>`;
});
