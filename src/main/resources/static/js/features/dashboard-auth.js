const AUTH_KEY = 'daln.authorization';

export async function requireDashboardRole(allowedRoles) {
  const authorization = sessionStorage.getItem(AUTH_KEY);
  if (!authorization) {
    window.location.replace('/');
    throw new Error('Bạn cần đăng nhập.');
  }

  const response = await fetch('/api/v1/auth/me', { headers: { Authorization: authorization } });
  if (!response.ok) {
    sessionStorage.removeItem(AUTH_KEY);
    window.location.replace('/');
    throw new Error('Phiên đăng nhập không hợp lệ.');
  }
  const user = await response.json();
  const role = user.roles.find(value => allowedRoles.includes(value));
  if (!role) {
    const destination = user.roles.includes('SUPER_ADMIN') ? '/admin.html' : user.roles.includes('CUSTOMER') ? '/user.html' : '/';
    window.location.replace(destination);
    throw new Error('Tài khoản không có quyền truy cập trang này.');
  }

  document.querySelectorAll('[data-logout]').forEach(button => button.addEventListener('click', () => {
    sessionStorage.removeItem(AUTH_KEY);
    window.location.replace('/');
  }));

  return {
    user,
    async request(url, options = {}) {
      const result = await fetch(url, {
        ...options,
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}), Authorization: authorization }
      });
      const body = await result.json().catch(() => ({}));
      if (!result.ok) throw new Error(body.detail || body.message || `Yêu cầu lỗi (${result.status})`);
      return body;
    }
  };
}
