package kz.youni4st.app

import android.content.SharedPreferences
import kz.youni4st.app.data.*
import org.junit.Assert.*
import org.junit.Test

class AccountLocalStoreTest {
    @Test fun secondAccountNeverReceivesFirstAccountsData() {
        val store=LocalStore(MemoryPrefs())
        store.resume=Resume(name="Guest");store.favorites=setOf("uni:g")
        store.activate("alice");assertEquals("Guest",store.resume.name)
        store.resume=Resume(name="Alice");store.favorites=setOf("uni:a");store.chat=listOf(ChatMessage("user","Private"))
        store.activate("bob")
        assertTrue(store.resume.name.isBlank());assertTrue(store.favorites.isEmpty());assertTrue(store.chat.isEmpty())
        store.activate("alice");assertEquals("Alice",store.resume.name);assertEquals(setOf("uni:a"),store.favorites)
        store.activate(null);assertEquals("Guest",store.resume.name)
    }
    @Test fun lateUploadAcknowledgementCannotEraseANewerOfflineSave() {
        val store=LocalStore(MemoryPrefs());store.activate("alice")
        store.resume=Resume(name="First");val sent=store.pending("alice")
        store.resume=Resume(name="Second");store.acknowledge("alice",sent)
        assertTrue("resume" in store.pending("alice"));assertEquals("Second",store.resume.name)
    }
    @Test fun pendingChangesSurviveAppRestartAndRemainUnderTheirOwner() {
        val prefs=MemoryPrefs();val before=LocalStore(prefs);before.activate("alice")
        before.favorites=setOf("uni:a");before.profile=UserProfile(name="Alice")
        val after=LocalStore(prefs);after.activate("bob");assertTrue(after.pending("bob").isEmpty())
        after.activate("alice");assertEquals(setOf("uni:a"),after.favorites)
        assertTrue("favorites|uni:a" in after.pending("alice"));assertTrue("profile" in after.pending("alice"))
    }
    @Test fun remoteRestoreDoesNotQueueAnUploadOfEverythingAgain() {
        val store=LocalStore(MemoryPrefs());store.activate("alice")
        store.withoutTracking {store.resume=Resume(name="Cloud");store.favorites=setOf("uni:cloud")}
        assertTrue(store.pending("alice").isEmpty());assertEquals("Cloud",store.resume.name)
    }
    @Test fun favoriteRemovalHasAnExplicitPendingTombstone() {
        val store=LocalStore(MemoryPrefs());store.activate("alice")
        store.withoutTracking {store.favorites=setOf("uni:a")};store.favorites=emptySet()
        assertTrue("favorites|uni:a" in store.pending("alice"));assertTrue(store.favorites.isEmpty())
    }
    @Test fun verifiedUsernameAndCloudFlagsAreAccountScoped() {
        val store=LocalStore(MemoryPrefs());store.activate("alice");store.rememberUsername("alice","wombly");store.markCloud("alice")
        store.activate("bob");assertEquals("",store.verifiedUsername("bob"));assertFalse(store.hasCloud("bob"))
        assertEquals("wombly",store.verifiedUsername("alice"));assertTrue(store.hasCloud("alice"))
    }
    @Test fun oldAccountProfileMigratesWithoutClaimingItForAnotherAccount() {
        val prefs=MemoryPrefs();prefs.edit().putString("profile_migrated_uid","alice").putString("profile:alice",UserProfile(name="Original").toJson().toString()).apply()
        val store=LocalStore(prefs);store.activate("bob");assertEquals("",store.profile.name)
        store.activate("alice");assertEquals("Original",store.profile.name)
    }
}

private class MemoryPrefs:SharedPreferences {
    private val data=mutableMapOf<String,Any>()
    override fun getAll():MutableMap<String,*> = data.toMutableMap()
    override fun getString(key:String?,defValue:String?):String?=data[key] as? String ?: defValue
    override fun getStringSet(key:String?,defValues:MutableSet<String>?):MutableSet<String>?=(data[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues
    override fun getInt(key:String?,defValue:Int)=data[key] as? Int ?: defValue
    override fun getLong(key:String?,defValue:Long)=data[key] as? Long ?: defValue
    override fun getFloat(key:String?,defValue:Float)=data[key] as? Float ?: defValue
    override fun getBoolean(key:String?,defValue:Boolean)=data[key] as? Boolean ?: defValue
    override fun contains(key:String?)=data.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener:SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener:SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun edit():SharedPreferences.Editor=object:SharedPreferences.Editor {
        private val writes=mutableMapOf<String,Any?>();private var clear=false
        private fun put(key:String?,value:Any?):SharedPreferences.Editor {writes[requireNotNull(key)]=value;return this}
        override fun putString(key:String?,value:String?)=put(key,value)
        override fun putStringSet(key:String?,values:MutableSet<String>?)=put(key,values?.toSet())
        override fun putInt(key:String?,value:Int)=put(key,value)
        override fun putLong(key:String?,value:Long)=put(key,value)
        override fun putFloat(key:String?,value:Float)=put(key,value)
        override fun putBoolean(key:String?,value:Boolean)=put(key,value)
        override fun remove(key:String?)=put(key,null)
        override fun clear():SharedPreferences.Editor {clear=true;return this}
        override fun commit():Boolean {apply();return true}
        override fun apply() {if(clear)data.clear();writes.forEach {(key,value)->if(value==null)data.remove(key) else data[key]=value}}
    }
}
