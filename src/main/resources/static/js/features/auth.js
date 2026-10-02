export function createAuth() {
  const AUTH_KEY = 'daln.authorization';
  let authorization = sessionStorage.getItem(AUTH_KEY);
  let currentUser = null;
  const dialog = document.querySelector('#auth-dialog');
  const message = document.querySelector('#auth-message');
  const authLink = document.querySelector('#auth-link');

  function selectMode(mode) {
    const registering = mode === 'register';
    document.querySelector('#login-form').hidden = registering;
    document.querySelector('#register-form').hidden = !registering;
    document.querySelector('#auth-title').textContent = registering ? 'Tạo tài khoản khách hàng' : 'Đăng nhập';
    document.querySelectorAll('.auth-tab').forEach(tab => tab.classList.toggle('active', tab.dataset.authMode === mode));
    message.textContent = '';
  }

  function encodeBasic(email, password) {
    const bytes = new TextEncoder().encode(`${email.trim()}:${password}`);
    let binary = '';
    bytes.forEach(byte => { binary += String.fromCharCode(byte); });
    return `Basic ${btoa(binary)}`;
  }

  async function verifyCredentials(email, password) {
    const header = encodeBasic(email, password);
    const response = await fetch('/api/v1/auth/me', { headers: { Authorization: header } });
    if (!response.ok) throw new Error(response.status === 401 ? 'Email hoặc mật khẩu không đúng.' : 'Không thể đăng nhập lúc này.');
    return { header, user: await response.json() };
  }

  function setUser(user, header) {
    authorization = header;
    currentUser = user;
    sessionStorage.setItem(AUTH_KEY, header);
    authLink.textContent = `${user.email} · Đăng xuất`;
    authLink.title = `Vai trò: ${user.roles.join(', ')}`;
  }

  function dashboardFor(user) {
    if (user.roles.includes('SUPER_ADMIN')) return '/admin.html';
    if (user.roles.includes('CUSTOMER')) return '/user.html';
    return '/';
  }

  function open() {
    selectMode('login');
    if (!dialog.open) dialog.showModal();
  }

  document.querySelectorAll('.auth-tab').forEach(tab => tab.addEventListener('click', () => selectMode(tab.dataset.authMode)));
  document.querySelector('#auth-dialog .dialog-close').addEventListener('click', () => dialog.close());
  document.querySelector('#open-auth-from-hold').addEventListener('click', open);

  authLink.addEventListener('click', event => {
    event.preventDefault();
    if (authorization) {
      authorization = null;
      currentUser = null;
      sessionStorage.removeItem(AUTH_KEY);
      authLink.textContent = 'Đăng nhập';
      authLink.removeAttribute('title');
      message.textContent = 'Bạn đã đăng xuất.';
      return;
    }
    open();
  });

  document.querySelector('#login-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const submit = form.querySelector('[type="submit"]');
    submit.disabled = true;
    message.textContent = 'Đang xác thực…';
    try {
      const result = await verifyCredentials(data.get('email'), data.get('password'));
      setUser(result.user, result.header);
      form.reset();
      dialog.close();
      window.location.assign(dashboardFor(result.user));
    } catch (error) {
      message.textContent = error.message;
    } finally {
      submit.disabled = false;
    }
  });

  document.querySelector('#register-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const submit = form.querySelector('[type="submit"]');
    submit.disabled = true;
    message.textContent = 'Đang tạo tài khoản…';
    try {
      const response = await fetch('/api/v1/auth/register', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fullName: data.get('fullName'), email: data.get('email'), password: data.get('password') })
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(result.detail || 'Không thể tạo tài khoản.');
      const login = await verifyCredentials(data.get('email'), data.get('password'));
      setUser(login.user, login.header);
      form.reset();
      dialog.close();
      window.location.assign(dashboardFor(login.user));
    } catch (error) {
      message.textContent = error.message;
    } finally {
      submit.disabled = false;
    }
  });

  async function initialize() {
    if (!authorization) return;
    try {
      const response = await fetch('/api/v1/auth/me', { headers: { Authorization: authorization } });
      if (!response.ok) throw new Error('Expired auth');
      setUser(await response.json(), authorization);
    } catch {
      authorization = null;
      currentUser = null;
      sessionStorage.removeItem(AUTH_KEY);
      authLink.textContent = 'Đăng nhập';
    }
  }

  return { open, initialize, authorizationHeader: () => authorization, user: () => currentUser };
}
