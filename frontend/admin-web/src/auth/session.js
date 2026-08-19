const KEY = 'music-dreamer.admin-session';
const fields = ['token', 'expiresAt', 'userId', 'role', 'status'];
export function loadAdminSession(storage, now=Date.now()){const raw=storage.getItem(KEY);if(!raw)return null;try{const value=JSON.parse(raw);if(value.role!=='ADMIN'||value.status!=='ACTIVE'||Date.parse(value.expiresAt)<=now){storage.removeItem(KEY);return null;}return Object.fromEntries(fields.map(field=>[field,value[field]]));}catch{storage.removeItem(KEY);return null;}}
export function saveAdminSession(storage,value){if(value?.role!=='ADMIN'||value.status!=='ACTIVE')throw Object.assign(new Error('ADMIN_REQUIRED'),{status:403});const session=Object.fromEntries(fields.map(field=>[field,value[field]]));storage.setItem(KEY,JSON.stringify(session));return session;}
export function clearAdminSession(storage){storage.removeItem(KEY);}
