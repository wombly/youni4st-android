import {initializeTestEnvironment,assertFails,assertSucceeds} from '@firebase/rules-unit-testing';
import {doc,getDoc,setDoc,getDocs,collection,deleteDoc,setLogLevel} from 'firebase/firestore';
import {createMockUserToken} from '@firebase/util';
import {readFile} from 'node:fs/promises';
import {resolve} from 'node:path';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';
setLogLevel('silent');
const env=await initializeTestEnvironment({projectId:'demo-youni4st',firestore:{host:'127.0.0.1',port:8089,rules:await readFile(resolve(process.env.YOUNI_RULES??'firestore.rules'),'utf8')}});
const db=uid=>env.authenticatedContext(uid).firestore();
let passed=0;
async function test(name,run){await env.clearFirestore();await run();passed++;console.log('PASS '+name);}
const data=()=>({schemaVersion:1,profile:{name:'Alice',photo:'',interests:['IT']},resume:{name:'Alice',photo:'',position:'Developer'},favorites:{},drafts:{},chat:[{role:'user',content:'Question'}],settings:{language:'RU'},receipts:{}});
const field=v=>v===null?{nullValue:'NULL_VALUE'}:Array.isArray(v)?{arrayValue:{values:v.map(field)}}:typeof v==='object'?{mapValue:{fields:Object.fromEntries(Object.entries(v).map(([k,x])=>[k,field(x)]))}}:typeof v==='boolean'?{booleanValue:v}:typeof v==='number'?{integerValue:String(v)}:{stringValue:v};
const hash=id=>'i'+createHash('sha256').update(id).digest('hex');
async function patch(uid,values,paths) {
 const url=new URL('http://127.0.0.1:8089/v1/projects/demo-youni4st/databases/(default)/documents/user_data/'+uid);
 for(const path of [...paths,'schemaVersion'])url.searchParams.append('updateMask.fieldPaths',path);
 return fetch(url,{method:'PATCH',headers:{'Content-Type':'application/json',Authorization:'Bearer '+createMockUserToken({sub:uid},'demo-youni4st')},body:JSON.stringify({fields:field({...values,schemaVersion:1}).mapValue.fields})});
}
try {
 await test('private data restored by same UID on a second device',async()=>{
  const first=db('alice'),second=db('alice');await assertSucceeds(setDoc(doc(first,'user_data','alice'),data()));
  const restored=(await assertSucceeds(getDoc(doc(second,'user_data','alice')))).data();
  assert.equal(restored.resume.position,'Developer');assert.deepEqual(restored.profile.interests,['IT']);assert.equal(restored.chat[0].content,'Question');
 });
 await test('another account cannot read or write private data',async()=>{
  await setDoc(doc(db('alice'),'user_data','alice'),data());
  await assertFails(getDoc(doc(db('bob'),'user_data','alice')));
  await assertFails(setDoc(doc(db('bob'),'user_data','alice'),data()));
 });
 await test('unauthenticated access and enumeration denied',async()=>{
  await setDoc(doc(db('alice'),'user_data','alice'),data());
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(),'user_data','alice')));
  await assertFails(getDocs(collection(db('alice'),'user_data')));
 });
 await test('Android REST patches preserve simultaneous favorites from two devices',async()=>{
  await setDoc(doc(db('alice'),'user_data','alice'),data());
  const [one,two]=await Promise.all([
   patch('alice',{favorites:{[hash('uni:a')]:{id:'uni:a',selected:true}}},['favorites.'+hash('uni:a')]),
   patch('alice',{favorites:{[hash('uni:b')]:{id:'uni:b',selected:true}}},['favorites.'+hash('uni:b')])
  ]);
  assert.equal(one.status,200,await one.text());assert.equal(two.status,200,await two.text());
  const restored=(await getDoc(doc(db('alice'),'user_data','alice'))).data();
  assert.equal(Object.keys(restored.favorites).length,2);assert.equal(restored.resume.position,'Developer');
  const removal=await patch('alice',{favorites:{[hash('uni:a')]:{id:'uni:a',selected:false}}},['favorites.'+hash('uni:a')]);assert.equal(removal.status,200);
  const after=(await getDoc(doc(db('alice'),'user_data','alice'))).data();assert.equal(after.favorites[hash('uni:a')].selected,false);assert.equal(after.favorites[hash('uni:b')].selected,true);
 });
 await test('partial resume save does not erase profile or chat',async()=>{
  await setDoc(doc(db('alice'),'user_data','alice'),data());
  const result=await patch('alice',{resume:{name:'Alice',photo:'',position:'Designer'}},['resume']);assert.equal(result.status,200,await result.text());
  const restored=(await getDoc(doc(db('alice'),'user_data','alice'))).data();assert.equal(restored.profile.name,'Alice');assert.equal(restored.chat.length,1);assert.equal(restored.resume.position,'Designer');
 });
 await test('invalid photo paths and oversized photos rejected',async()=>{
  const bad=data();bad.profile.photo='file:///another-phone/photo.jpg';await assertFails(setDoc(doc(db('alice'),'user_data','alice'),bad));
  bad.profile.photo='jpeg:'+'A'.repeat(180000);await assertFails(setDoc(doc(db('alice'),'user_data','alice'),bad));
 });
 await test('unknown fields and unsupported versions rejected',async()=>{
  await assertFails(setDoc(doc(db('alice'),'user_data','alice'),{...data(),isAdmin:true}));
  await assertFails(setDoc(doc(db('alice'),'user_data','alice'),{...data(),schemaVersion:2}));
 });
 await test('private data cannot change username ownership',async()=>{
  await setDoc(doc(db('alice'),'user_data','alice'),data());
  await assertFails(setDoc(doc(db('alice'),'handles','bob'),{username:'stolen'}));
  await assertFails(deleteDoc(doc(db('alice'),'user_data','alice')));
 });
 console.log(`${passed} cloud account security tests passed.`);
} finally {await env.cleanup();}
