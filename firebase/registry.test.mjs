import {initializeTestEnvironment, assertFails, assertSucceeds} from '@firebase/rules-unit-testing';
import {doc, getDoc, setDoc, deleteDoc, writeBatch, setLogLevel} from 'firebase/firestore';
import {readFile} from 'node:fs/promises';
import assert from 'node:assert/strict';
import {resolve} from 'node:path';
import {createMockUserToken} from '@firebase/util';
setLogLevel('silent');
const rules=await readFile(resolve(process.env.YOUNI_RULES ?? 'firestore.rules'),'utf8');
const env=await initializeTestEnvironment({projectId:'demo-youni4st',firestore:{host:'127.0.0.1',port:8089,rules}});
let passed=0;
const db=uid=>env.authenticatedContext(uid).firestore();
function claim(database,uid,name,old) {
 const batch=writeBatch(database);
 batch.set(doc(database,'usernames',name),{uid});
 batch.set(doc(database,'handles',uid),{username:name});
 if(old)batch.delete(doc(database,'usernames',old));
 return batch.commit();
}
async function test(name,run){await env.clearFirestore();await run();passed++;console.log('PASS '+name);}
try {
 await test('unauthenticated claim denied',async()=>{
  await assertFails(claim(env.unauthenticatedContext().firestore(),'alice','wombly'));
 });
 await test('atomic claim succeeds and duplicate is denied',async()=>{
  await assertSucceeds(claim(db('alice'),'alice','wombly'));
  await assertFails(claim(db('bob'),'bob','wombly'));
  assert.equal((await getDoc(doc(db('alice'),'usernames','wombly'))).data().uid,'alice');
 });
 await test('cannot impersonate another UID',async()=>{
  await assertFails(claim(db('bob'),'alice','wombly'));
 });
 await test('standalone pointer and standalone reservation denied',async()=>{
  await assertFails(setDoc(doc(db('alice'),'usernames','wombly'),{uid:'alice'}));
  await assertFails(setDoc(doc(db('alice'),'handles','alice'),{username:'wombly'}));
 });
 await test('uppercase, invalid names and unexpected fields denied',async()=>{
  for(const name of ['Wombly','ab','1name','a'.repeat(25)]) await assertFails(claim(db('alice'),'alice',name));
  const d=db('alice'), batch=writeBatch(d);
  batch.set(doc(d,'usernames','wombly'),{uid:'alice',admin:true});batch.set(doc(d,'handles','alice'),{username:'wombly'});
  await assertFails(batch.commit());
 });
 await test('rename releases old name and reserves new name atomically',async()=>{
  const a=db('alice');await assertSucceeds(claim(a,'alice','wombly'));
  await assertSucceeds(claim(a,'alice','newname','wombly'));
  assert.equal((await getDoc(doc(a,'usernames','wombly'))).exists(),false);
  await assertSucceeds(claim(db('bob'),'bob','wombly'));
 });
 await test('cannot hoard names or delete active reservation',async()=>{
  const a=db('alice');await claim(a,'alice','wombly');
  await assertFails(claim(a,'alice','othername'));
  await assertFails(deleteDoc(doc(a,'usernames','wombly')));
  await assertFails(deleteDoc(doc(a,'handles','alice')));
 });
 await test('one winner in concurrent registration',async()=>{
  const results=await Promise.allSettled([claim(db('alice'),'alice','shared'),claim(db('bob'),'bob','shared')]);
  assert.equal(results.filter(x=>x.status==='fulfilled').length,1);
 });
 await test('private profile pointers cannot be read by others',async()=>{
  await claim(db('alice'),'alice','wombly');
  await assertFails(getDoc(doc(db('bob'),'handles','alice')));
  await assertSucceeds(getDoc(doc(db('alice'),'handles','alice')));
 });
 await test('cannot rename into somebody else’s name',async()=>{
  await claim(db('alice'),'alice','wombly');await claim(db('bob'),'bob','other');
  await assertFails(claim(db('bob'),'bob','wombly','other'));
  assert.equal((await getDoc(doc(db('bob'),'handles','bob'))).data().username,'other');
 });
 await test('Android REST commit enforces uniqueness with Firebase ID tokens',async()=>{
  const root='projects/demo-youni4st/databases/(default)/documents';
  const headers=uid=>({'Content-Type':'application/json','Authorization':'Bearer '+createMockUserToken({sub:uid},'demo-youni4st')});
  const commit=uid=>fetch(`http://127.0.0.1:8089/v1/${root}:commit`,{method:'POST',headers:headers(uid),body:JSON.stringify({writes:[
   {update:{name:`${root}/usernames/restname`,fields:{uid:{stringValue:uid}}},currentDocument:{exists:false}},
   {update:{name:`${root}/handles/${uid}`,fields:{username:{stringValue:'restname'}}},currentDocument:{exists:false}}
  ]})});
  const created=await commit('alice');assert.equal(created.status,200,await created.text());
  const denied=await commit('bob');const detail=await denied.json();
  assert.equal(denied.status,409,JSON.stringify(detail));assert.equal(detail.error.status,'ALREADY_EXISTS');
  const own=await fetch(`http://127.0.0.1:8089/v1/${root}/handles/alice`,{headers:headers('alice')});
  assert.equal(own.status,200);assert.equal((await own.json()).fields.username.stringValue,'restname');
 });
 console.log(`${passed} Firestore security tests passed.`);
} finally {await env.cleanup();}
