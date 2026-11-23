// Staging check: a scripted player through the staging Velocity, lobby <-> smp, LuckPerms set by the Panel console.
// Needs PTERO_TOKEN (staging Panel key of `claude`) in the environment and ssh access to read the server logs; never prints the key.
// Usage: PTERO_TOKEN=... node staging-check.js [name]
const mineflayer=require('mineflayer');const {execSync}=require('child_process');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const SSH='fearland@100.94.60.16',VOL='/srv/pterodactyl/volumes/';
const SRV={lobby:{id:'38f208d3',dir:'38f208d3-17e6-40c5-875e-bd155d6a91fa'},smp:{id:'ecfdbb88',dir:'ecfdbb88-6c3c-4ca1-ab8d-10a89a93a004'}};
const NAME=process.argv[2]||'hmc_stg_a1';
const cmd=async(s,c)=>{const r=await fetch(`https://staging.fearland.com.br/api/client/servers/${SRV[s].id}/command`,{method:'POST',headers:{Authorization:'Bearer '+process.env.PTERO_TOKEN,'Content-Type':'application/json',Accept:'application/json'},body:JSON.stringify({command:c})});return r.status};
const LOGF=s=>`${VOL}${SRV[s].dir}/logs/latest.log`;
const ssh=c=>{try{return execSync(`ssh ${SSH} "${c}"`,{encoding:'utf8'}).trim()}catch(e){return ''}};
// Line count of a server log now; later reads look only at lines written after it (a join from an earlier run cannot satisfy a check).
const mark=s=>parseInt(ssh(`sudo wc -l < ${LOGF(s)}`),10)||0;
// Paper writes "UUID of player <name> is <uuid>" when this server accepts the connection: the destination server, the exact player.
const joinedSince=(s,from,uuid)=>ssh(`sudo tail -n +${from+1} ${LOGF(s)} | grep -F 'UUID of player ${NAME} is ${uuid}'`);
const results=[];const ok=(n,c,d='')=>{results.push([c?'PASS':'FAIL',n,d]);console.log(c?'PASS':'FAIL',n,d)};
function connect(){return new Promise(res=>{
 const b=mineflayer.createBot({host:'100.94.60.16',port:25565,username:NAME,version:'1.21.11',auth:'offline',physicsEnabled:false});
 b.log=[];const c=b._client;const w=c.write.bind(c);
 c.on('system_chat',p=>{const t=[];JSON.stringify(p).replace(/"text":\{"type":"string","value":"([^"]*)"/g,(m,x)=>t.push(x));b.log.push(t.join(''))});
 c.write=(n,p)=>{if(['position','position_look','look','flying','player_loaded'].includes(n))return;return w(n,p)};
 c.on('position',p=>{try{w('teleport_confirm',{teleportId:p.teleportId})}catch(e){}});
 c.on('add_resource_pack',p=>{console.log('resource pack offered');for(const r of [3,4,0])try{w('resource_pack_receive',{uuid:p.uuid,result:r})}catch(e){}});
 b.once('spawn',()=>res(b));b.on('error',e=>console.log('err',e.message));b.on('kicked',k=>console.log('kicked',JSON.stringify(k).slice(0,150)));
})}
const say=async(b,c,wait=1500)=>{b.log.length=0;b.chat(c);await sleep(wait);return b.log.join(' | ')};
const has=(s,r)=>new RegExp(r,'i').test(s);
(async()=>{
 let b=await connect();await sleep(4000);
 console.log(await say(b,'/register Fl_pass_123 Fl_pass_123',4000));console.log(await say(b,'/login Fl_pass_123',3000));await sleep(6000);
 const id=b.player.uuid;const lp=(a)=>Promise.all(['lobby','smp'].map(s=>cmd(s,`lp user ${id} ${a}`)));
 await lp('permission clear');await sleep(2500);
 let r=await say(b,'/cosmetic apply top_hat');ok('no perms denied',has(r,'do not have permission'),r.slice(-100));
 await lp('permission set fearland.cosmetics.top_hat true');await sleep(2500);
 r=await say(b,'/cosmetic apply top_hat');ok('item only denied',has(r,'do not have permission'),r.slice(-100));
 await lp('permission set fearland.cosmetics.hats true');await lp('permission set fearland.cosmetics.backpacks true');await lp('permission set fearland.cosmetics.glacial_crown true');await sleep(2500);
 r=await say(b,'/cosmetic apply dragon_hat');ok('category only denied',has(r,'do not have permission'),r.slice(-100));
 r=await say(b,'/cosmetic apply top_hat');ok('both perms equips (Nexo)',has(r,'(?<!un)equipped top_hat'),r.slice(-100));
 r=await say(b,'/cosmetic apply backpack_beehive');ok('backpack category only equips (Nexo)',has(r,'(?<!un)equipped backpack_beehive'),r.slice(-100));
 r=await say(b,'/cosmetic apply glacial_crown');ok('crown equips (Nexo)',has(r,'(?<!un)equipped glacial_crown'),r.slice(-100));
 await sleep(2500);
 const m=mark('smp');b.chat('/server smp');await sleep(9000);
 const j=joinedSince('smp',m,id);ok('on smp: this player UUID joined smp after the transfer (server log)',j!=='',j);
 r=await say(b,'/cosmetic unapply HELMET',2000);ok('lobby->smp: equipment follows from PostgreSQL',has(r,'unequipped glacial_crown'),r.slice(-120));
 r=await say(b,'/cosmetic apply top_hat',1500);ok('smp equip',has(r,'(?<!un)equipped top_hat'),r.slice(-100));
 b.chat('/server lobby');await sleep(9000);
 r=await say(b,'/cosmetic unapply BACKPACK',2000);ok('smp->lobby: backpack followed',has(r,'unequipped backpack_beehive'),r.slice(-120));
 b.end();await sleep(2500);
 b=await connect();await sleep(5000);console.log(await say(b,'/login Fl_pass_123',3000));await sleep(6000);
 r=await say(b,'/cosmetic unapply HELMET',2000);ok('reconnect: helmet persisted',has(r,'unequipped top_hat'),r.slice(-120));
 b.end();
 if(!process.env.RESTART){console.log(JSON.stringify(results));process.exit(results.some(x=>x[0]==='FAIL')?1:0)}
 // lobby restart: equipment survives a server restart (PostgreSQL)
 b=await connect();await sleep(5000);console.log(await say(b,'/login Fl_pass_123',3000));await sleep(6000);
 await say(b,'/cosmetic apply glacial_crown',1200);await say(b,'/cosmetic apply backpack_beehive',1200);await sleep(2500);
 console.log('restart lobby',await fetch(`https://staging.fearland.com.br/api/client/servers/${SRV.lobby.id}/power`,{method:'POST',headers:{Authorization:'Bearer '+process.env.PTERO_TOKEN,'Content-Type':'application/json'},body:JSON.stringify({signal:'restart'})}).then(r=>r.status));
 b.end();await sleep(20000);
 await sleep(170000); // Paper boots in about 150 s on staging
 b=await connect();await sleep(5000);console.log(await say(b,'/login Fl_pass_123',3000));await sleep(6000);
 r=await say(b,'/cosmetic unapply HELMET',2000);ok('after lobby restart: helmet persisted',has(r,'unequipped glacial_crown'),r.slice(-120));
 r=await say(b,'/cosmetic unapply BACKPACK',2000);ok('after lobby restart: backpack persisted',has(r,'unequipped backpack_beehive'),r.slice(-120));
 b.end();
 console.log(JSON.stringify(results));process.exit(results.some(x=>x[0]==='FAIL')?1:0);
})();
