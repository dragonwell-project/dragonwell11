/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#include "precompiled.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/dictionary.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "logging/log.hpp"
#include "logging/logMessage.hpp"
#include "memory/heapInspection.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/metaspaceShared.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/perfData.hpp"
#include "utilities/macros.hpp"
#include "utilities/rotate_bits.hpp"
#include "utilities/population_count.hpp"
#include "utilities/stack.inline.hpp"
#include "utilities/count_trailing_zeros.hpp"

void Klass::set_java_mirror(Handle m) {
  assert(!m.is_null(), "New mirror should never be null.");
  assert(_java_mirror.resolve() == NULL, "should only be used to initialize mirror");
  _java_mirror = class_loader_data()->add_handle(m);
}

oop Klass::java_mirror() const {
  return _java_mirror.resolve();
}

oop Klass::java_mirror_no_keepalive() const {
  return _java_mirror.peek();
}

bool Klass::is_cloneable() const {
  return _access_flags.is_cloneable_fast() ||
         is_subtype_of(SystemDictionary::Cloneable_klass());
}

void Klass::set_is_cloneable() {
  if (name() == vmSymbols::java_lang_invoke_MemberName()) {
    assert(is_final(), "no subclasses allowed");
    // MemberName cloning should not be intrinsified and always happen in JVM_Clone.
  } else if (is_instance_klass() && InstanceKlass::cast(this)->reference_type() != REF_NONE) {
    // Reference cloning should not be intrinsified and always happen in JVM_Clone.
  } else {
    _access_flags.set_is_cloneable_fast();
  }
}

uint8_t Klass::compute_hash_slot(Symbol* n) {
  uint hash_code;
  // Special cases for the two superclasses of all Array instances.
  // Code elsewhere assumes, for all instances of ArrayKlass, that
  // these two interfaces will be in this order.

  // We ensure there are some empty slots in the hash table between
  // these two very common interfaces because if they were adjacent
  // (e.g. Slots 0 and 1), then any other class which hashed to 0 or 1
  // would result in a probe length of 3.
  if (n == vmSymbols::java_lang_Cloneable()) {
    hash_code = 0;
  } else if (n == vmSymbols::java_io_Serializable()) {
    hash_code = SECONDARY_SUPERS_TABLE_SIZE / 2;
  } else {
    const jbyte* s = static_cast<const jbyte*>(n->bytes());
    hash_code = java_lang_String::hash_code(s, n->utf8_length());
    // We use String::hash_code here (rather than e.g.
    // Symbol::identity_hash()) in order to have a hash code that
    // does not change from run to run. We want that because the
    // hash value for a secondary superclass appears in generated
    // code as a constant.

    // This constant is magic: see Knuth, "Fibonacci Hashing".
    const uint multiplier
      = 2654435769; // (uint)(((u8)1 << 32) / ((1 + sqrt(5)) / 2 ))
    const uint hash_shift = sizeof(hash_code) * 8 - 6;
    // The leading bits of the least significant half of the product.
    hash_code = (hash_code * multiplier) >> hash_shift;

    if (StressSecondarySupers) {
      // Generate many hash collisions in order to stress-test the
      // linear search fallback.
      hash_code = hash_code % 3;
      hash_code = hash_code * (SECONDARY_SUPERS_TABLE_SIZE / 3);
    }
  }

  return (hash_code & SECONDARY_SUPERS_TABLE_MASK);
}

void Klass::set_name(Symbol* n) {
  _name = n;
   if (_name != NULL) {
    _name->increment_refcount();
  }

  if (UseSecondarySupersTable) {
    elapsedTimer selftime;
    selftime.start();

    _hash_slot = compute_hash_slot(n);
    assert(_hash_slot < SECONDARY_SUPERS_TABLE_SIZE, "required");

    selftime.stop();
    if (UsePerfData) {
      ClassLoader::perf_secondary_hash_time()->inc(selftime.ticks());
    }
  }
}

bool Klass::is_subclass_of(const Klass* k) const {
  // Run up the super chain and check
  if (this == k) return true;

  Klass* t = const_cast<Klass*>(this)->super();

  while (t != NULL) {
    if (t == k) return true;
    t = t->super();
  }
  return false;
}

bool Klass::search_secondary_supers(Klass* k) const {
  // Put some extra logic here out-of-line, before the search proper.
  // This cuts down the size of the inline method.

  // This is necessary, since I am never in my own secondary_super list.
  if (this == k)
    return true;
  // Scan the array-of-objects for a match
  int cnt = secondary_supers()->length();
  for (int i = 0; i < cnt; i++) {
    if (secondary_supers()->at(i) == k) {
      ((Klass*)this)->set_secondary_super_cache(k);
      return true;
    }
  }
  return false;
}

// Return self, except for abstract classes with exactly 1
// implementor.  Then return the 1 concrete implementation.
Klass *Klass::up_cast_abstract() {
  Klass *r = this;
  while( r->is_abstract() ) {   // Receiver is abstract?
    Klass *s = r->subklass();   // Check for exactly 1 subklass
    if( !s || s->next_sibling() ) // Oops; wrong count; give up
      return this;              // Return 'this' as a no-progress flag
    r = s;                    // Loop till find concrete class
  }
  return r;                   // Return the 1 concrete class
}

// Find LCA in class hierarchy
Klass *Klass::LCA( Klass *k2 ) {
  Klass *k1 = this;
  while( 1 ) {
    if( k1->is_subtype_of(k2) ) return k2;
    if( k2->is_subtype_of(k1) ) return k1;
    k1 = k1->super();
    k2 = k2->super();
  }
}


void Klass::check_valid_for_instantiation(bool throwError, TRAPS) {
  ResourceMark rm(THREAD);
  THROW_MSG(throwError ? vmSymbols::java_lang_InstantiationError()
            : vmSymbols::java_lang_InstantiationException(), external_name());
}


void Klass::copy_array(arrayOop s, int src_pos, arrayOop d, int dst_pos, int length, TRAPS) {
  ResourceMark rm(THREAD);
  assert(s != NULL, "Throw NPE!");
  THROW_MSG(vmSymbols::java_lang_ArrayStoreException(),
            err_msg("arraycopy: source type %s is not an array", s->klass()->external_name()));
}


void Klass::initialize(TRAPS) {
  ShouldNotReachHere();
}

bool Klass::compute_is_subtype_of(Klass* k) {
  assert(k->is_klass(), "argument must be a class");
  return is_subclass_of(k);
}

Klass* Klass::find_field(Symbol* name, Symbol* sig, fieldDescriptor* fd) const {
#ifdef ASSERT
  tty->print_cr("Error: find_field called on a klass oop."
                " Likely error: reflection method does not correctly"
                " wrap return value in a mirror object.");
#endif
  ShouldNotReachHere();
  return NULL;
}

Method* Klass::uncached_lookup_method(const Symbol* name, const Symbol* signature,
                                      OverpassLookupMode overpass_mode,
                                      PrivateLookupMode private_mode) const {
#ifdef ASSERT
  tty->print_cr("Error: uncached_lookup_method called on a klass oop."
                " Likely error: reflection method does not correctly"
                " wrap return value in a mirror object.");
#endif
  ShouldNotReachHere();
  return NULL;
}

void* Klass::operator new(size_t size, ClassLoaderData* loader_data, size_t word_size, TRAPS) throw() {
  return Metaspace::allocate(loader_data, word_size, MetaspaceObj::ClassType, THREAD);
}

static markOop make_prototype(Klass* kls) {
  markOop prototype = markOopDesc::prototype();
#ifdef _LP64
  if (UseCompactObjectHeaders) {
    prototype = prototype->set_klass(kls);
  }
#endif
  return prototype;
}

// "Normal" instantiation is preceeded by a MetaspaceObj allocation
// which zeros out memory - calloc equivalent.
// The constructor is also used from CppVtableCloner,
// which doesn't zero out the memory before calling the constructor.
// Need to set the _java_mirror field explicitly to not hit an assert that the field
// should be NULL before setting it.
Klass::Klass(KlassID id) : _id(id),
                           _prototype_header(make_prototype(this)),
                           _shared_class_path_index(-1),
#if INCLUDE_JVMTI
                           _shared_unregistered_class_path_index(-1),
#endif
                           _java_mirror(NULL) {
  CDS_ONLY(_shared_class_flags = 0;)
  CDS_JAVA_HEAP_ONLY(_archived_mirror = 0;)
  _primary_supers[0] = this;
  set_super_check_offset(in_bytes(primary_supers_offset()));
}

jint Klass::array_layout_helper(BasicType etype) {
  assert(etype >= T_BOOLEAN && etype <= T_OBJECT, "valid etype");
  // Note that T_ARRAY is not allowed here.
  int  hsize = arrayOopDesc::base_offset_in_bytes(etype);
  int  esize = type2aelembytes(etype);
  bool isobj = (etype == T_OBJECT);
  int  tag   =  isobj ? _lh_array_tag_obj_value : _lh_array_tag_type_value;
  int lh = array_layout_helper(tag, hsize, etype, exact_log2(esize));

  assert(lh < (int)_lh_neutral_value, "must look like an array layout");
  assert(layout_helper_is_array(lh), "correct kind");
  assert(layout_helper_is_objArray(lh) == isobj, "correct kind");
  assert(layout_helper_is_typeArray(lh) == !isobj, "correct kind");
  assert(layout_helper_header_size(lh) == hsize, "correct decode");
  assert(layout_helper_element_type(lh) == etype, "correct decode");
  assert(1 << layout_helper_log2_element_size(lh) == esize, "correct decode");

  return lh;
}

bool Klass::can_be_primary_super_slow() const {
  if (super() == NULL)
    return true;
  else if (super()->super_depth() >= primary_super_limit()-1)
    return false;
  else
    return true;
}

void Klass::set_secondary_supers(Array<Klass*>* secondaries) {
  assert(!UseSecondarySupersTable || secondaries == NULL, "");
  set_secondary_supers(secondaries, SECONDARY_SUPERS_BITMAP_EMPTY);
}

void Klass::set_secondary_supers(Array<Klass*>* secondaries, uintx bitmap) {
#ifdef ASSERT
  if (UseSecondarySupersTable && secondaries != NULL) {
    uintx real_bitmap = compute_secondary_supers_bitmap(secondaries);
    assert(bitmap == real_bitmap, "must be");
  }
#endif
  _bitmap = bitmap;
  _secondary_supers = secondaries;

  /* if (secondaries != NULL) {
    LogMessage(class, load) msg;
    LogStream log {LogLevel::Debug, msg};
    if (log.is_enabled()) {
      ResourceMark rm;
      log.print_cr("set_secondary_supers: hash_slot: %d; klass: %s", hash_slot(), external_name());
      print_secondary_supers_on(&log);
    }
  } */
}

// Hashed secondary superclasses
//
// We use a compressed 64-entry hash table with linear probing. We
// start by creating a hash table in the usual way, followed by a pass
// that removes all the null entries. To indicate which entries would
// have been null we use a bitmap that contains a 1 in each position
// where an entry is present, 0 otherwise. This bitmap also serves as
// a kind of Bloom filter, which in many cases allows us quickly to
// eliminate the possibility that something is a member of a set of
// secondaries.
uintx Klass::hash_secondary_supers(Array<Klass*>* secondaries, bool rewrite) {
  const int length = secondaries->length();

  if (length == 0) {
    return SECONDARY_SUPERS_BITMAP_EMPTY;
  }

  if (length == 1) {
    int hash_slot = secondaries->at(0)->hash_slot();
    return uintx(1) << hash_slot;
  }

  // For performance reasons we don't use a hashed table unless there
  // are at least two empty slots in it. If there were only one empty
  // slot it'd take a long time to create the table and the resulting
  // search would be no faster than linear probing.
  if (length > SECONDARY_SUPERS_TABLE_SIZE - 2) {
    return SECONDARY_SUPERS_BITMAP_FULL;
  }

  {
    PerfTraceTime ptt(ClassLoader::perf_secondary_hash_time());

    ResourceMark rm;
    uintx bitmap = SECONDARY_SUPERS_BITMAP_EMPTY;
    GrowableArray<Klass*>* hashed_secondaries = new GrowableArray<Klass*>(SECONDARY_SUPERS_TABLE_SIZE,
                                                        SECONDARY_SUPERS_TABLE_SIZE, NULL);

    for (int j = 0; j < length; j++) {
      Klass* k = secondaries->at(j);
      hash_insert(k, hashed_secondaries, bitmap);
    }

    // Pack the hashed secondaries array by copying it into the
    // secondaries array, sans nulls, if modification is allowed.
    // Otherwise, validate the order.
    int i = 0;
    for (int slot = 0; slot < SECONDARY_SUPERS_TABLE_SIZE; slot++) {
      bool has_element = ((bitmap >> slot) & 1) != 0;
      assert(has_element == (hashed_secondaries->at(slot) != NULL), "");
      if (has_element) {
        Klass* k = hashed_secondaries->at(slot);
        if (rewrite) {
          secondaries->at_put(i, k);
        } else if (secondaries->at(i) != k) {
          assert(false, "broken secondary supers hash table");
          return SECONDARY_SUPERS_BITMAP_FULL;
        }
        i++;
      }
    }
    assert(i == secondaries->length(), "mismatch");

    return bitmap;
  }
}

void Klass::hash_insert(Klass* klass, GrowableArray<Klass*>* secondaries, uintx& bitmap) {
  assert(bitmap != SECONDARY_SUPERS_BITMAP_FULL, "");

  int dist = 0;
  for (int slot = klass->hash_slot(); true; slot = (slot + 1) & SECONDARY_SUPERS_TABLE_MASK) {
    Klass* existing = secondaries->at(slot);
    assert(((bitmap >> slot) & 1) == (existing != NULL), "mismatch");
    if (existing == NULL) { // no conflict
      secondaries->at_put(slot, klass);
      bitmap |= uintx(1) << slot;
      assert(bitmap != SECONDARY_SUPERS_BITMAP_FULL, "");
      return;
    } else {
      // Use Robin Hood hashing to minimize the worst case search.
      // Also, every permutation of the insertion sequence produces
      // the same final Robin Hood hash table, provided that a
      // consistent tie breaker is used.
      int existing_dist = (slot - existing->hash_slot()) & SECONDARY_SUPERS_TABLE_MASK;
      if (existing_dist < dist
          // This tie breaker ensures that the hash order is maintained.
          || ((existing_dist == dist)
              && (uintptr_t(existing) < uintptr_t(klass)))) {
        Klass* tmp = secondaries->at(slot);
        secondaries->at_put(slot, klass);
        klass = tmp;
        dist = existing_dist;
      }
      ++dist;
    }
  }
}

Array<Klass*>* Klass::pack_secondary_supers(ClassLoaderData* loader_data,
                                            GrowableArray<Klass*>* primaries,
                                            GrowableArray<Klass*>* secondaries,
                                            uintx& bitmap, TRAPS) {
  int new_length = primaries->length() + secondaries->length();
  Array<Klass*>* secondary_supers = MetadataFactory::new_array<Klass*>(loader_data, new_length, CHECK_NULL);

  // Combine the two arrays into a metadata object to pack the array.
  // The primaries are added in the reverse order, then the secondaries.
  int fill_p = primaries->length();
  for (int j = 0; j < fill_p; j++) {
    secondary_supers->at_put(j, primaries->pop());  // add primaries in reverse order.
  }
  for( int j = 0; j < secondaries->length(); j++ ) {
    secondary_supers->at_put(j+fill_p, secondaries->at(j));  // add secondaries on the end.
  }
#ifdef ASSERT
  // We must not copy any null placeholders left over from bootstrap.
  for (int j = 0; j < secondary_supers->length(); j++) {
    assert(secondary_supers->at(j) != NULL, "correct bootstrapping order");
  }
#endif

  if (UseSecondarySupersTable) {
    bitmap = hash_secondary_supers(secondary_supers, /*rewrite=*/true); // rewrites freshly allocated array
  } else {
    bitmap = SECONDARY_SUPERS_BITMAP_EMPTY;
  }
  return secondary_supers;
}

uintx Klass::compute_secondary_supers_bitmap(Array<Klass*>* secondary_supers) {
  return hash_secondary_supers(secondary_supers, /*rewrite=*/false); // no rewrites allowed
}

uint8_t Klass::compute_home_slot(Klass* k, uintx bitmap) {
  uint8_t hash = k->hash_slot();
  if (hash > 0) {
    return population_count(bitmap << (SECONDARY_SUPERS_TABLE_SIZE - hash));
  }
  return 0;
}

void Klass::initialize_supers(Klass* k, Array<Klass*>* transitive_interfaces, TRAPS) {
  if (FastSuperclassLimit == 0) {
    // None of the other machinery matters.
    set_super(k);
    return;
  }
  if (k == NULL) {
    set_super(NULL);
    _primary_supers[0] = this;
    assert(super_depth() == 0, "Object must already be initialized properly");
  } else if (k != super() || k == SystemDictionary::Object_klass()) {
    assert(super() == NULL || super() == SystemDictionary::Object_klass(),
           "initialize this only once to a non-trivial value");
    set_super(k);
    Klass* sup = k;
    int sup_depth = sup->super_depth();
    juint my_depth  = MIN2(sup_depth + 1, (int)primary_super_limit());
    if (!can_be_primary_super_slow())
      my_depth = primary_super_limit();
    for (juint i = 0; i < my_depth; i++) {
      _primary_supers[i] = sup->_primary_supers[i];
    }
    Klass* *super_check_cell;
    if (my_depth < primary_super_limit()) {
      _primary_supers[my_depth] = this;
      super_check_cell = &_primary_supers[my_depth];
    } else {
      // Overflow of the primary_supers array forces me to be secondary.
      super_check_cell = &_secondary_super_cache;
    }
    set_super_check_offset((address)super_check_cell - (address) this);

#ifdef ASSERT
    {
      juint j = super_depth();
      assert(j == my_depth, "computed accessor gets right answer");
      Klass* t = this;
      while (!t->can_be_primary_super()) {
        t = t->super();
        j = t->super_depth();
      }
      for (juint j1 = j+1; j1 < primary_super_limit(); j1++) {
        assert(primary_super_of_depth(j1) == NULL, "super list padding");
      }
      while (t != NULL) {
        assert(primary_super_of_depth(j) == t, "super list initialization");
        t = t->super();
        --j;
      }
      assert(j == (juint)-1, "correct depth count");
    }
#endif
  }

  if (secondary_supers() == NULL) {

    // Now compute the list of secondary supertypes.
    // Secondaries can occasionally be on the super chain,
    // if the inline "_primary_supers" array overflows.
    int extras = 0;
    Klass* p;
    for (p = super(); !(p == NULL || p->can_be_primary_super()); p = p->super()) {
      ++extras;
    }

    ResourceMark rm(THREAD);  // need to reclaim GrowableArrays allocated below

    // Compute the "real" non-extra secondaries.
    GrowableArray<Klass*>* secondaries = compute_secondary_supers(extras, transitive_interfaces);
    if (secondaries == NULL) {
      // secondary_supers set by compute_secondary_supers
      return;
    }

    GrowableArray<Klass*>* primaries = new GrowableArray<Klass*>(extras);

    for (p = super(); !(p == NULL || p->can_be_primary_super()); p = p->super()) {
      int i;                    // Scan for overflow primaries being duplicates of 2nd'arys

      // This happens frequently for very deeply nested arrays: the
      // primary superclass chain overflows into the secondary.  The
      // secondary list contains the element_klass's secondaries with
      // an extra array dimension added.  If the element_klass's
      // secondary list already contains some primary overflows, they
      // (with the extra level of array-ness) will collide with the
      // normal primary superclass overflows.
      for( i = 0; i < secondaries->length(); i++ ) {
        if( secondaries->at(i) == p )
          break;
      }
      if( i < secondaries->length() )
        continue;               // It's a dup, don't put it in
      primaries->push(p);
    }
    // Combine the two arrays into a metadata object to pack the array.
    uintx bitmap = 0;
    Array<Klass*>* s2 = pack_secondary_supers(class_loader_data(), primaries, secondaries, bitmap, CHECK);
    set_secondary_supers(s2, bitmap);
  }
}

GrowableArray<Klass*>* Klass::compute_secondary_supers(int num_extra_slots,
                                                       Array<Klass*>* transitive_interfaces) {
  assert(num_extra_slots == 0, "override for complex klasses");
  assert(transitive_interfaces == NULL, "sanity");
  set_secondary_supers(Universe::the_empty_klass_array(), Universe::the_empty_klass_bitmap());
  return NULL;
}


InstanceKlass* Klass::superklass() const {
  assert(super() == NULL || super()->is_instance_klass(), "must be instance klass");
  return _super == NULL ? NULL : InstanceKlass::cast(_super);
}

void Klass::set_subklass(Klass* s) {
  assert(s != this, "sanity check");
  _subklass = s;
}

void Klass::set_next_sibling(Klass* s) {
  assert(s != this, "sanity check");
  _next_sibling = s;
}

void Klass::append_to_sibling_list() {
  debug_only(verify();)
  // add ourselves to superklass' subklass list
  InstanceKlass* super = superklass();
  if (super == NULL) return;        // special case: class Object
  assert((!super->is_interface()    // interfaces cannot be supers
          && (super->superklass() == NULL || !is_interface())),
         "an interface can only be a subklass of Object");
  Klass* prev_first_subklass = super->subklass();
  if (prev_first_subklass != NULL) {
    // set our sibling to be the superklass' previous first subklass
    set_next_sibling(prev_first_subklass);
  }
  // make ourselves the superklass' first subklass
  super->set_subklass(this);
  debug_only(verify();)
}

oop Klass::holder_phantom() const {
  return class_loader_data()->holder_phantom();
}

void Klass::clean_weak_klass_links(bool unloading_occurred, bool clean_alive_klasses) {
  if (!ClassUnloading || !unloading_occurred) {
    return;
  }

  Klass* root = SystemDictionary::Object_klass();
  Stack<Klass*, mtGC> stack;

  stack.push(root);
  while (!stack.is_empty()) {
    Klass* current = stack.pop();

    assert(current->is_loader_alive(), "just checking, this should be live");

    // Find and set the first alive subklass
    Klass* sub = current->subklass();
    while (sub != NULL && !sub->is_loader_alive()) {
#ifndef PRODUCT
      if (log_is_enabled(Trace, class, unload)) {
        ResourceMark rm;
        log_trace(class, unload)("unlinking class (subclass): %s", sub->external_name());
      }
#endif
      sub = sub->next_sibling();
    }
    current->set_subklass(sub);
    if (sub != NULL) {
      stack.push(sub);
    }

    // Find and set the first alive sibling
    Klass* sibling = current->next_sibling();
    while (sibling != NULL && !sibling->is_loader_alive()) {
      if (log_is_enabled(Trace, class, unload)) {
        ResourceMark rm;
        log_trace(class, unload)("[Unlinking class (sibling) %s]", sibling->external_name());
      }
      sibling = sibling->next_sibling();
    }
    current->set_next_sibling(sibling);
    if (sibling != NULL) {
      stack.push(sibling);
    }

    // Clean the implementors list and method data.
    if (clean_alive_klasses && current->is_instance_klass()) {
      InstanceKlass* ik = InstanceKlass::cast(current);
      ik->clean_weak_instanceklass_links();

      // JVMTI RedefineClasses creates previous versions that are not in
      // the class hierarchy, so process them here.
      while ((ik = ik->previous_versions()) != NULL) {
        ik->clean_weak_instanceklass_links();
      }
    }
  }
}

void Klass::metaspace_pointers_do(MetaspaceClosure* it) {
  if (log_is_enabled(Trace, cds)) {
    ResourceMark rm;
    log_trace(cds)("Iter(Klass): %p (%s)", this, external_name());
  }

  it->push(&_name);
  it->push(&_secondary_super_cache);
  it->push(&_secondary_supers);
  for (int i = 0; i < _primary_super_limit; i++) {
    it->push(&_primary_supers[i]);
  }
  it->push(&_super);
  it->push(&_subklass);
  it->push(&_next_sibling);
  it->push(&_next_link);

  vtableEntry* vt = start_of_vtable();
  for (int i=0; i<vtable_length(); i++) {
    it->push(vt[i].method_addr());
  }
}

void Klass::remove_unshareable_info() {
  assert (DumpSharedSpaces, "only called for DumpSharedSpaces");
  JFR_ONLY(REMOVE_ID(this);)
  if (log_is_enabled(Trace, cds, unshareable)) {
    ResourceMark rm;
    log_trace(cds, unshareable)("remove: %s", external_name());
  }

  set_subklass(NULL);
  set_next_sibling(NULL);
  set_next_link(NULL);

  // Null out class_loader_data because we don't share that yet.
  set_class_loader_data(NULL);
  set_is_shared();

  // FIXME: validation in Klass::hash_secondary_supers() may fail for shared klasses.
  // Even though the bitmaps always match, the canonical order of elements in the table
  // is not guaranteed to stay the same (see tie breaker during Robin Hood hashing in Klass::hash_insert).
  //assert(compute_secondary_supers_bitmap(secondary_supers()) == _bitmap, "broken table");
}

void Klass::remove_java_mirror() {
  assert (DumpSharedSpaces, "only called for DumpSharedSpaces");
  if (log_is_enabled(Trace, cds, unshareable)) {
    ResourceMark rm;
    log_trace(cds, unshareable)("remove java_mirror: %s", external_name());
  }
  // Just null out the mirror.  The class_loader_data() no longer exists.
  _java_mirror = NULL;
}

void Klass::restore_unshareable_info(ClassLoaderData* loader_data, Handle protection_domain, TRAPS) {
  assert(is_klass(), "ensure C++ vtable is restored");
  assert(is_shared(), "must be set");
  JFR_ONLY(RESTORE_ID(this);)
  if (log_is_enabled(Trace, cds, unshareable)) {
    ResourceMark rm;
    log_trace(cds, unshareable)("restore: %s", external_name());
  }

  // If an exception happened during CDS restore, some of these fields may already be
  // set.  We leave the class on the CLD list, even if incomplete so that we don't
  // modify the CLD list outside a safepoint.
  if (class_loader_data() == NULL) {
    // Restore class_loader_data to the null class loader data
    set_class_loader_data(loader_data);

    // Add to null class loader list first before creating the mirror
    // (same order as class file parsing)
    loader_data->add_class(this);
  }

  Handle loader(THREAD, loader_data->class_loader());
  ModuleEntry* module_entry = NULL;
  Klass* k = this;
  if (k->is_objArray_klass()) {
    k = ObjArrayKlass::cast(k)->bottom_klass();
  }
  // Obtain klass' module.
  if (k->is_instance_klass()) {
    InstanceKlass* ik = (InstanceKlass*) k;
    module_entry = ik->module();
  } else {
    module_entry = ModuleEntryTable::javabase_moduleEntry();
  }
  // Obtain java.lang.Module, if available
  Handle module_handle(THREAD, ((module_entry != NULL) ? module_entry->module() : (oop)NULL));

  if (this->has_raw_archived_mirror()) {
    ResourceMark rm;
    log_debug(cds, mirror)("%s has raw archived mirror", external_name());
    if (MetaspaceShared::open_archive_heap_region_mapped()) {
      bool present = java_lang_Class::restore_archived_mirror(this, loader, module_handle,
                                                              protection_domain,
                                                              CHECK);
      if (present) {
        return;
      }
    }

    // No archived mirror data
    log_debug(cds, mirror)("No archived mirror data for %s", external_name());
    _java_mirror = NULL;
    this->clear_has_raw_archived_mirror();
  }

  // Only recreate it if not present.  A previous attempt to restore may have
  // gotten an OOM later but keep the mirror if it was created.
  if (java_mirror() == NULL) {
    log_trace(cds, mirror)("Recreate mirror for %s", external_name());
    java_lang_Class::create_mirror(this, loader, module_handle, protection_domain, CHECK);
  }
}

#if INCLUDE_CDS_JAVA_HEAP
// Used at CDS dump time to access the archived mirror. No GC barrier.
oop Klass::archived_java_mirror_raw() {
  assert(has_raw_archived_mirror(), "must have raw archived mirror");
  return CompressedOops::decode(_archived_mirror);
}

narrowOop Klass::archived_java_mirror_raw_narrow() {
  assert(has_raw_archived_mirror(), "must have raw archived mirror");
  return _archived_mirror;
}

// No GC barrier
void Klass::set_archived_java_mirror_raw(oop m) {
  assert(DumpSharedSpaces, "called only during runtime");
  _archived_mirror = CompressedOops::encode(m);
}
#endif // INCLUDE_CDS_JAVA_HEAP

Klass* Klass::array_klass_or_null(int rank) {
  EXCEPTION_MARK;
  // No exception can be thrown by array_klass_impl when called with or_null == true.
  // (In anycase, the execption mark will fail if it do so)
  return array_klass_impl(true, rank, THREAD);
}


Klass* Klass::array_klass_or_null() {
  EXCEPTION_MARK;
  // No exception can be thrown by array_klass_impl when called with or_null == true.
  // (In anycase, the execption mark will fail if it do so)
  return array_klass_impl(true, THREAD);
}


Klass* Klass::array_klass_impl(bool or_null, int rank, TRAPS) {
  fatal("array_klass should be dispatched to InstanceKlass, ObjArrayKlass or TypeArrayKlass");
  return NULL;
}


Klass* Klass::array_klass_impl(bool or_null, TRAPS) {
  fatal("array_klass should be dispatched to InstanceKlass, ObjArrayKlass or TypeArrayKlass");
  return NULL;
}

oop Klass::class_loader() const { return class_loader_data()->class_loader(); }

// In product mode, this function doesn't have virtual function calls so
// there might be some performance advantage to handling InstanceKlass here.
const char* Klass::external_name() const {
  if (is_instance_klass()) {
    const InstanceKlass* ik = static_cast<const InstanceKlass*>(this);
    if (ik->is_anonymous()) {
      char addr_buf[20];
      jio_snprintf(addr_buf, 20, "/" INTPTR_FORMAT, p2i(ik));
      size_t addr_len = strlen(addr_buf);
      size_t name_len = name()->utf8_length();
      char*  result   = NEW_RESOURCE_ARRAY(char, name_len + addr_len + 1);
      name()->as_klass_external_name(result, (int) name_len + 1);
      assert(strlen(result) == name_len, "");
      strcpy(result + name_len, addr_buf);
      assert(strlen(result) == name_len + addr_len, "");
      return result;
    }
  }
  if (name() == NULL)  return "<unknown>";
  return name()->as_klass_external_name();
}

const char* Klass::signature_name() const {
  if (name() == NULL)  return "<unknown>";
  return name()->as_C_string();
}

const char* Klass::external_kind() const {
  if (is_interface()) return "interface";
  if (is_abstract()) return "abstract class";
  return "class";
}

// Unless overridden, modifier_flags is 0.
jint Klass::compute_modifier_flags(TRAPS) const {
  return 0;
}

int Klass::atomic_incr_biased_lock_revocation_count() {
  return (int) Atomic::add(1, &_biased_lock_revocation_count);
}

// Unless overridden, jvmti_class_status has no flags set.
jint Klass::jvmti_class_status() const {
  return 0;
}


// Printing

void Klass::print_on(outputStream* st) const {
  ResourceMark rm;
  // print title
  st->print("%s", internal_name());
  print_address_on(st);
  st->cr();
}

void Klass::oop_print_on(oop obj, outputStream* st) {
  ResourceMark rm;
  // print title
  st->print_cr("%s ", internal_name());
  obj->print_address_on(st);

  if (WizardMode) {
     // print header
     obj->mark()->print_on(st);
     if (UseCompactObjectHeaders) {
       st->cr();
       st->print(" - prototype_header: " INTPTR_FORMAT, _prototype_header->value());
       st->cr();
     }
  }

  // print class
  st->print(" - klass: ");
  obj->klass()->print_value_on(st);
  st->cr();
}

void Klass::oop_print_value_on(oop obj, outputStream* st) {
  // print title
  ResourceMark rm;              // Cannot print in debug mode without this
  st->print("%s", internal_name());
  obj->print_address_on(st);
}

#if INCLUDE_SERVICES
// Size Statistics
void Klass::collect_statistics(KlassSizeStats *sz) const {
  sz->_klass_bytes = sz->count(this);
  sz->_mirror_bytes = sz->count(java_mirror_no_keepalive());
  sz->_secondary_supers_bytes = sz->count_array(secondary_supers());

  sz->_ro_bytes += sz->_secondary_supers_bytes;
  sz->_rw_bytes += sz->_klass_bytes + sz->_mirror_bytes;
}
#endif // INCLUDE_SERVICES

// Verification

void Klass::verify_on(outputStream* st) {

  // This can be expensive, but it is worth checking that this klass is actually
  // in the CLD graph but not in production.
  assert(Metaspace::contains((address)this), "Should be");

  guarantee(this->is_klass(),"should be klass");

  if (super() != NULL) {
    guarantee(super()->is_klass(), "should be klass");
  }
  if (secondary_super_cache() != NULL) {
    Klass* ko = secondary_super_cache();
    guarantee(ko->is_klass(), "should be klass");
  }
  for ( uint i = 0; i < primary_super_limit(); i++ ) {
    Klass* ko = _primary_supers[i];
    if (ko != NULL) {
      guarantee(ko->is_klass(), "should be klass");
    }
  }

  if (java_mirror_no_keepalive() != NULL) {
    guarantee(java_lang_Class::is_instance(java_mirror_no_keepalive()), "should be instance");
  }
}

void Klass::oop_verify_on(oop obj, outputStream* st) {
  guarantee(oopDesc::is_oop(obj),  "should be oop");
  guarantee(obj->klass()->is_klass(), "klass field is not a klass");
}

Klass* Klass::decode_klass_raw(narrowKlass narrow_klass) {
  return (Klass*)(void*)( (uintptr_t)Universe::narrow_klass_base() +
                         ((uintptr_t)narrow_klass << Universe::narrow_klass_shift()));
}

bool Klass::is_valid(Klass* k) {
  if (!is_aligned(k, sizeof(MetaWord))) return false;
  if ((size_t)k < os::min_page_size()) return false;

  if (!os::is_readable_range(k, k + 1)) return false;
  if (!MetaspaceUtils::is_range_in_committed(k, k + 1)) return false;

  if (!Symbol::is_valid(k->name())) return false;
  return ClassLoaderDataGraph::is_valid(k->class_loader_data());
}

klassVtable Klass::vtable() const {
  return klassVtable(const_cast<Klass*>(this), start_of_vtable(), vtable_length() / vtableEntry::size());
}

vtableEntry* Klass::start_of_vtable() const {
  return (vtableEntry*) ((address)this + in_bytes(vtable_start_offset()));
}

Method* Klass::method_at_vtable(int index)  {
#ifndef PRODUCT
  assert(index >= 0, "valid vtable index");
  if (DebugVtables) {
    verify_vtable_index(index);
  }
#endif
  return start_of_vtable()[index].method();
}

ByteSize Klass::vtable_start_offset() {
  return in_ByteSize(InstanceKlass::header_size() * wordSize);
}

#ifndef PRODUCT

bool Klass::verify_vtable_index(int i) {
  int limit = vtable_length()/vtableEntry::size();
  assert(i >= 0 && i < limit, "index %d out of bounds %d", i, limit);
  return true;
}

bool Klass::verify_itable_index(int i) {
  assert(is_instance_klass(), "");
  int method_count = klassItable::method_count_for_interface(this);
  assert(i >= 0 && i < method_count, "index out of bounds");
  return true;
}

#endif // PRODUCT

// Caller needs ResourceMark
// joint_in_module_of_loader provides an optimization if 2 classes are in
// the same module to succinctly print out relevant information about their
// module name and class loader's name_and_id for error messages.
// Format:
//   <fully-qualified-external-class-name1> and <fully-qualified-external-class-name2>
//                      are in module <module-name>[@<version>]
//                      of loader <loader-name_and_id>[, parent loader <parent-loader-name_and_id>]
const char* Klass::joint_in_module_of_loader(const Klass* class2, bool include_parent_loader) const {
  assert(module() == class2->module(), "classes do not have the same module");
  const char* class1_name = external_name();
  size_t len = strlen(class1_name) + 1;

  const char* class2_description = class2->class_in_module_of_loader(true, include_parent_loader);
  len += strlen(class2_description);

  len += strlen(" and ");

  char* joint_description = NEW_RESOURCE_ARRAY_RETURN_NULL(char, len);

  // Just return the FQN if error when allocating string
  if (joint_description == NULL) {
    return class1_name;
  }

  jio_snprintf(joint_description, len, "%s and %s",
               class1_name,
               class2_description);

  return joint_description;
}

// Caller needs ResourceMark
// class_in_module_of_loader provides a standard way to include
// relevant information about a class, such as its module name as
// well as its class loader's name_and_id, in error messages and logging.
// Format:
//   <fully-qualified-external-class-name> is in module <module-name>[@<version>]
//                                         of loader <loader-name_and_id>[, parent loader <parent-loader-name_and_id>]
const char* Klass::class_in_module_of_loader(bool use_are, bool include_parent_loader) const {
  // 1. fully qualified external name of class
  const char* klass_name = external_name();
  size_t len = strlen(klass_name) + 1;

  // 2. module name + @version
  const char* module_name = "";
  const char* version = "";
  bool has_version = false;
  bool module_is_named = false;
  const char* module_name_phrase = "";
  const Klass* bottom_klass = is_objArray_klass() ?
                                ObjArrayKlass::cast(this)->bottom_klass() : this;
  if (bottom_klass->is_instance_klass()) {
    ModuleEntry* module = InstanceKlass::cast(bottom_klass)->module();
    if (module->is_named()) {
      module_is_named = true;
      module_name_phrase = "module ";
      module_name = module->name()->as_C_string();
      len += strlen(module_name);
      // Use version if exists and is not a jdk module
      if (module->should_show_version()) {
        has_version = true;
        version = module->version()->as_C_string();
        // Include stlen(version) + 1 for the "@"
        len += strlen(version) + 1;
      }
    } else {
      module_name = UNNAMED_MODULE;
      len += UNNAMED_MODULE_LEN;
    }
  } else {
    // klass is an array of primitives, module is java.base
    module_is_named = true;
    module_name_phrase = "module ";
    module_name = JAVA_BASE_NAME;
    len += JAVA_BASE_NAME_LEN;
  }

  // 3. class loader's name_and_id
  ClassLoaderData* cld = class_loader_data();
  assert(cld != NULL, "class_loader_data should not be null");
  const char* loader_name_and_id = cld->loader_name_and_id();
  len += strlen(loader_name_and_id);

  // 4. include parent loader information
  const char* parent_loader_phrase = "";
  const char* parent_loader_name_and_id = "";
  if (include_parent_loader &&
      !cld->is_builtin_class_loader_data()) {
    oop parent_loader = java_lang_ClassLoader::parent(class_loader());
    ClassLoaderData *parent_cld = ClassLoaderData::class_loader_data_or_null(parent_loader);
    // The parent loader's ClassLoaderData could be null if it is
    // a delegating class loader that has never defined a class.
    // In this case the loader's name must be obtained via the parent loader's oop.
    if (parent_cld == NULL) {
      oop cl_name_and_id = java_lang_ClassLoader::nameAndId(parent_loader);
      if (cl_name_and_id != NULL) {
        parent_loader_name_and_id = java_lang_String::as_utf8_string(cl_name_and_id);
      }
    } else {
      parent_loader_name_and_id = parent_cld->loader_name_and_id();
    }
    parent_loader_phrase = ", parent loader ";
    len += strlen(parent_loader_phrase) + strlen(parent_loader_name_and_id);
  }

  // Start to construct final full class description string
  len += ((use_are) ? strlen(" are in ") : strlen(" is in "));
  len += strlen(module_name_phrase) + strlen(" of loader ");

  char* class_description = NEW_RESOURCE_ARRAY_RETURN_NULL(char, len);

  // Just return the FQN if error when allocating string
  if (class_description == NULL) {
    return klass_name;
  }

  jio_snprintf(class_description, len, "%s %s in %s%s%s%s of loader %s%s%s",
               klass_name,
               (use_are) ? "are" : "is",
               module_name_phrase,
               module_name,
               (has_version) ? "@" : "",
               (has_version) ? version : "",
               loader_name_and_id,
               parent_loader_phrase,
               parent_loader_name_and_id);

  return class_description;
}

class LookupStats : StackObj {
 private:
  uint _no_of_samples;
  uint _worst;
  uint _worst_count;
  uint _average;
  uint _best;
  uint _best_count;
 public:
  LookupStats() : _no_of_samples(0), _worst(0), _worst_count(0), _average(0), _best(INT_MAX), _best_count(0) {}

  ~LookupStats() {
    assert(_best <= _worst || _no_of_samples == 0, "sanity");
  }

  void sample(uint value) {
    ++_no_of_samples;
    _average += value;

    if (_worst < value) {
      _worst = value;
      _worst_count = 1;
    } else if (_worst == value) {
      ++_worst_count;
    }

    if (_best > value) {
      _best = value;
      _best_count = 1;
    } else if (_best == value) {
      ++_best_count;
    }
  }

  void print_on(outputStream* st) const {
    st->print("best: %2d (%4.1f%%)", _best, (100.0 * _best_count) / _no_of_samples);
    if (_best_count < _no_of_samples) {
      st->print("; average: %4.1f; worst: %2d (%4.1f%%)",
                (1.0 * _average) / _no_of_samples,
                _worst, (100.0 * _worst_count) / _no_of_samples);
    }
  }
};

static void print_positive_lookup_stats(Array<Klass*>* secondary_supers, uintx bitmap, outputStream* st) {
  int num_of_supers = secondary_supers->length();

  LookupStats s;
  for (int i = 0; i < num_of_supers; i++) {
    Klass* secondary_super = secondary_supers->at(i);
    int home_slot = Klass::compute_home_slot(secondary_super, bitmap);
    uint score = 1 + ((i - home_slot) & Klass::SECONDARY_SUPERS_TABLE_MASK);
    s.sample(score);
  }
  st->print("positive_lookup: "); s.print_on(st);
}

static uint compute_distance_to_nearest_zero(int slot, uintx bitmap) {
  assert(~bitmap != 0, "no zeroes");
  uintx start = rotate_right(bitmap, slot);
  return count_trailing_zeros(~start);
}

static void print_negative_lookup_stats(uintx bitmap, outputStream* st) {
  LookupStats s;
  for (int slot = 0; slot < Klass::SECONDARY_SUPERS_TABLE_SIZE; slot++) {
    uint score = compute_distance_to_nearest_zero(slot, bitmap);
    s.sample(score);
  }
  st->print("negative_lookup: "); s.print_on(st);
}

void Klass::print_secondary_supers_on(outputStream* st) const {
  if (secondary_supers() != NULL) {
    if (UseSecondarySupersTable) {
      st->print("  - "); st->print("%d elements;", _secondary_supers->length());
      st->print_cr(" bitmap: " UINTX_FORMAT_X_0 ";", _bitmap);
      if (_bitmap != SECONDARY_SUPERS_BITMAP_EMPTY &&
          _bitmap != SECONDARY_SUPERS_BITMAP_FULL) {
        st->print("  - "); print_positive_lookup_stats(secondary_supers(), _bitmap, st); st->cr();
        st->print("  - "); print_negative_lookup_stats(_bitmap, st); st->cr();
      }
    }
  } else {
    st->print("null");
  }
}

void Klass::on_secondary_supers_verification_failure(Klass* super, Klass* sub, bool linear_result, bool table_result, const char* msg) {
  ResourceMark rm;
  super->print();
  sub->print();
  fatal("%s: %s implements %s: is_subtype_of: %d; linear_search: %d; table_lookup: %d",
        msg, sub->external_name(), super->external_name(),
        sub->is_subtype_of(super), linear_result, table_result);
}
