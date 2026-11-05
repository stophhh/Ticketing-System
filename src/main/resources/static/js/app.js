// 서버 세션을 인증 기준으로 사용합니다. localStorage 값은 화면 표시용입니다.
(() => {
  const nativeFetch = window.fetch.bind(window);
  let csrfPromise;
  const token = () => csrfPromise ||= nativeFetch('/users/csrf', {credentials:'same-origin'})
    .then(r => { if(!r.ok) throw new Error('요청 준비에 실패했습니다.'); return r.json(); })
    .catch(e => { csrfPromise=undefined; throw e; });
  window.escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  window.statusLabel = value => ({PENDING:'결제 대기',COMPLETED:'예매 완료',CANCELED:'취소 완료'}[value] || value);
  window.safeImage = value => {
    try { const u=new URL(value,location.origin); return ['http:','https:'].includes(u.protocol) ? escapeHtml(u.href) : '/images/default.svg'; }
    catch { return '/images/default.svg'; }
  };
  window.showApiError = message => {
    let box=document.getElementById('api-error');
    if(!box) { box=document.createElement('div');box.id='api-error';box.role='alert';
      box.style.cssText='position:fixed;bottom:20px;left:5%;right:5%;padding:16px;background:#fff1f2;color:#9f1239;border:1px solid #fecdd3;border-radius:12px;z-index:9999';
      document.body.appendChild(box); }
    box.textContent=message;
  };
  window.fetch = async (input,options={}) => {
    const url=new URL(typeof input==='string' ? input : input.url,location.href);
    if(url.origin!==location.origin) return nativeFetch(input,options);
    const method=(options.method || 'GET').toUpperCase();
    const headers=new Headers(options.headers);
    if(!['GET','HEAD','OPTIONS'].includes(method)) { const t=await token();headers.set(t.headerName,t.token); }
    const response=await nativeFetch(input,{...options,headers,credentials:'same-origin'});
    if(['/users/login','/users/logout'].includes(url.pathname)) csrfPromise=undefined;
    if(response.status===401 && !['/users/login','/users/me'].includes(url.pathname)) {
      localStorage.removeItem('userId');localStorage.removeItem('username');
      localStorage.setItem('redirectUrl',location.pathname+location.search);location.href='/login.html';
      throw new Error('로그인이 필요합니다.');
    }
    if(response.status===403 && url.pathname==='/users/csrf') csrfPromise=undefined;
    if(!response.ok && url.pathname!=='/users/me') {
      let message='요청을 처리하지 못했습니다.';
      try { const data=await response.clone().json();message=data.message || data.detail || data.error || message; } catch {}
      if(document.body) showApiError(message);
      throw new Error(message);
    }
    return response;
  };
  document.addEventListener('DOMContentLoaded', async () => {
    const res=await nativeFetch('/users/me',{credentials:'same-origin'});
    if(!res.ok) {localStorage.removeItem('userId');localStorage.removeItem('username');return;}
    const user=await res.json();
    localStorage.setItem('userId',String(user.userId));localStorage.setItem('username',user.username);
    if(user.role!=='ADMIN') for(const link of document.querySelectorAll('nav a[href="admin.html"]')) link.hidden=true;
    for(const link of document.querySelectorAll('nav a[href="login.html"]')) {
      link.textContent=user.username+' · 로그아웃';link.href='#';
      link.addEventListener('click',async e=>{e.preventDefault();await fetch('/users/logout',{method:'POST'});localStorage.removeItem('userId');localStorage.removeItem('username');location.href='/mainpage.html';});
    }
  });
})();
