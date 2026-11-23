const mineflayer=require('mineflayer');const {execSync}=require('child_process');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const sh=c=>{try{return execSync(c,{encoding:'utf8',stdio:['ignore','pipe','pipe']}).trim()}catch(e){return 'ERR '+e.message}};
const rcon=(s,c)=>sh(`docker exec fearland-dev-${s}-1 rcon-cli '${c}'`);
const psql=q=>sh(`docker exec fearland-dev-postgres-1 psql -U fearland -d fearland -At -c "${q}"`);
const NAME=process.argv[2]||'hmc_tst_a1';
const results=[];const ok=(n,c,d='')=>{results.push([c?'PASS':'FAIL',n,d]);console.log(c?'PASS':'FAIL',n,d)};
function connect(){return new Promise(res=>{
 const b=mineflayer.createBot({host:'127.0.0.1',port:25565,username:NAME,version:'1.21.11',auth:'offline',physicsEnabled:false});
 b.log=[];
 const c=b._client;const w=c.write.bind(c);
 c.on('system_chat',p=>{const t=[];JSON.stringify(p).replace(/"text":\{"type":"string","value":"([^"]*)"/g,(m,x)=>t.push(x));b.log.push(t.join(''))});
 c.write=(n,p)=>{if(['position','position_look','look','flying','player_loaded'].includes(n))return;return w(n,p)};
 c.on('position',p=>{try{w('teleport_confirm',{teleportId:p.teleportId})}catch(e){}});
 c.on('add_resource_pack',p=>{for(const r of [3,4,0])try{w('resource_pack_receive',{uuid:p.uuid,result:r})}catch(e){}});
 b.once('spawn',()=>res(b));b.on('error',e=>console.log('err',e.message));b.on('kicked',k=>console.log('kicked',JSON.stringify(k).slice(0,150)));
})}
const say=async(b,c,wait=1500)=>{b.log.length=0;b.chat(c);await sleep(wait);return b.log.join(' | ')};
const has=(s,r)=>new RegExp(r,'i').test(s);
// Exact row of this bot: its own UUID, never "any row".
const rowOf=b=>psql(`select 'row:'||cosmetics from cosmeticdatabase where uuid='${b.player.uuid}'`);
// The bot must be on the destination server, not merely have typed /server.
const onServer=(s,n)=>rcon(s,'list').includes(n);
(async()=>{
 let b=await connect();await sleep(4000);
 console.log('REG',await say(b,'/register Fl_pass_123 Fl_pass_123',4000));
 console.log(await say(b,'/login Fl_pass_123',3000));
 await sleep(4000);
 const pn=NAME;
 console.log('list',rcon('lobby','list'),rcon('smp','list'));
 rcon('lobby',`lp user ${pn} permission clear`);await sleep(1000);
 let r=await say(b,'/cosmetic apply top_hat');ok('no perms denied',has(r,'do not have permission'),r.slice(-120));
 rcon('lobby',`lp user ${pn} permission set fearland.cosmetics.top_hat true`);await sleep(500);
 r=await say(b,'/cosmetic apply top_hat');ok('item perm only (category missing) denied',has(r,'do not have permission'),r.slice(-120));
 rcon('lobby',`lp user ${pn} permission set fearland.cosmetics.hats true`);await sleep(500);
 r=await say(b,'/cosmetic apply dragon_hat');ok('category only (item missing) denied',has(r,'do not have permission'),r.slice(-120));
 r=await say(b,'/cosmetic apply top_hat');ok('both perms equips',has(r,'equipped top_hat'),r.slice(-120));
 rcon('lobby',`lp user ${pn} permission set fearland.cosmetics.mechanic_hat true`);await sleep(500);
 r=await say(b,'/cosmetic apply mechanic_hat');ok('item-only cosmetic (no category) equips',has(r,'equipped mechanic_hat'),r.slice(-120));
 r=await say(b,'/cosmetic apply top_hat');
 await sleep(2500);
 const uuid=rowOf(b);console.log('db',b.player.uuid,uuid);
 ok('postgres row written (exact uuid)',/^row:.*top_hat/.test(uuid),uuid);
 // dyeable cosmetic: the color is saved in the row and survives a transfer
 rcon('lobby',`lp user ${pn} permission set hmccosmetics.cmd.apply.color true`);rcon('lobby',`lp user ${pn} permission set hmccosmetics.cmd.dye true`);await sleep(800);
 r=await say(b,`/cosmetic apply dyed_cap ${pn} #FF0000`,1500);ok('dyed_cap equips with color',has(r,'equipped dyed_cap'),r.slice(-100));
 await sleep(2500);const dye=rowOf(b);ok('row keeps the dye color (FF0000 = 16711680)',/dyed_cap&16711680/.test(dye),dye);
 b.chat('/server smp');await sleep(7000);b.chat('/server lobby');await sleep(7000);
 await sleep(1000);const dye2=rowOf(b);ok('dye color unchanged after lobby->smp->lobby',/dyed_cap&16711680/.test(dye2),dye2);
 // fast transfer right after change
 r=await say(b,'/cosmetic apply top_hat',300);b.chat('/server smp');await sleep(7000);
 ok('bot is on smp',onServer('smp',pn),rcon('smp','list'));
 r=await say(b,'/cosmetic unapply HELMET',2000);ok('lobby->smp: equipment follows (fast transfer)',has(r,'unequipped top_hat'),r.slice(-150));
 r=await say(b,'/cosmetic apply top_hat',1500);ok('smp equip',has(r,'equipped top_hat'),r.slice(-100));
 b.chat('/server lobby');await sleep(7000);
 ok('bot is back on lobby',onServer('lobby',pn),rcon('lobby','list'));
 r=await say(b,'/cosmetic unapply HELMET',2000);ok('smp->lobby: equipment follows',has(r,'unequipped top_hat'),r.slice(-150));
 // postgres outage
 r=await say(b,'/cosmetic apply top_hat',1000);
 sh('docker stop fearland-dev-postgres-1');
 r=await say(b,'/cosmetic unapply HELMET',500);ok('equip change during outage does not crash',has(r,'unequipped'),r.slice(-100));
 await sleep(2000);sh('docker start fearland-dev-postgres-1');await sleep(15000);
 const row=rowOf(b);console.log('row after outage',JSON.stringify(row));
 ok('unequip made during the outage reached the row after it',/^row:/.test(row)&&!/top_hat/.test(row),row);
 b.end();await sleep(2000);
 b=await connect();await sleep(5000);
 console.log(await say(b,'/login Fl_pass_123',3000));await sleep(4000);
 r=await say(b,'/cosmetic apply dragon_hat',1500);console.log(r.slice(-150));
 b.end();
 console.log(rcon('lobby','list'));
 console.log(JSON.stringify(results));process.exit(results.some(r=>r[0]==='FAIL')?1:0);
})();
