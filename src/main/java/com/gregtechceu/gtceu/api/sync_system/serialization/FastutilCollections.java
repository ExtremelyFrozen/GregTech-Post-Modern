package com.gregtechceu.gtceu.api.sync_system.serialization;

import com.gregtechceu.gtceu.api.sync_system.serialization.CollectionCodecs.Construction;

import it.unimi.dsi.fastutil.BigList;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanArraySet;
import it.unimi.dsi.fastutil.booleans.BooleanBigArrayBigList;
import it.unimi.dsi.fastutil.booleans.BooleanBigList;
import it.unimi.dsi.fastutil.booleans.BooleanBigLists;
import it.unimi.dsi.fastutil.booleans.BooleanCollection;
import it.unimi.dsi.fastutil.booleans.BooleanCollections;
import it.unimi.dsi.fastutil.booleans.BooleanImmutableList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.booleans.BooleanLists;
import it.unimi.dsi.fastutil.booleans.BooleanOpenHashSet;
import it.unimi.dsi.fastutil.booleans.BooleanSet;
import it.unimi.dsi.fastutil.booleans.BooleanSets;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanMap;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanMaps;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanRBTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanSortedMap;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanSortedMaps;
import it.unimi.dsi.fastutil.bytes.Byte2ByteAVLTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2ByteArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ByteMap;
import it.unimi.dsi.fastutil.bytes.Byte2ByteMaps;
import it.unimi.dsi.fastutil.bytes.Byte2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ByteOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ByteRBTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2ByteSortedMap;
import it.unimi.dsi.fastutil.bytes.Byte2ByteSortedMaps;
import it.unimi.dsi.fastutil.bytes.Byte2CharAVLTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2CharArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2CharLinkedOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2CharMap;
import it.unimi.dsi.fastutil.bytes.Byte2CharMaps;
import it.unimi.dsi.fastutil.bytes.Byte2CharOpenCustomHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2CharOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2CharRBTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2CharSortedMap;
import it.unimi.dsi.fastutil.bytes.Byte2CharSortedMaps;
import it.unimi.dsi.fastutil.bytes.Byte2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2DoubleArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2DoubleMap;
import it.unimi.dsi.fastutil.bytes.Byte2DoubleMaps;
import it.unimi.dsi.fastutil.bytes.Byte2DoubleOpenCustomHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2DoubleSortedMap;
import it.unimi.dsi.fastutil.bytes.Byte2DoubleSortedMaps;
import it.unimi.dsi.fastutil.bytes.Byte2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2FloatArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2FloatMap;
import it.unimi.dsi.fastutil.bytes.Byte2FloatMaps;
import it.unimi.dsi.fastutil.bytes.Byte2FloatOpenCustomHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2FloatOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2FloatRBTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2FloatSortedMap;
import it.unimi.dsi.fastutil.bytes.Byte2FloatSortedMaps;
import it.unimi.dsi.fastutil.bytes.Byte2IntAVLTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2IntArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2IntMap;
import it.unimi.dsi.fastutil.bytes.Byte2IntMaps;
import it.unimi.dsi.fastutil.bytes.Byte2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2IntOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2IntRBTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2IntSortedMap;
import it.unimi.dsi.fastutil.bytes.Byte2IntSortedMaps;
import it.unimi.dsi.fastutil.bytes.Byte2LongAVLTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongMaps;
import it.unimi.dsi.fastutil.bytes.Byte2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongRBTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongSortedMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongSortedMaps;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMaps;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectSortedMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectSortedMaps;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceMap;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceMaps;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceSortedMap;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceSortedMaps;
import it.unimi.dsi.fastutil.bytes.Byte2ShortAVLTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2ShortArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ShortMap;
import it.unimi.dsi.fastutil.bytes.Byte2ShortMaps;
import it.unimi.dsi.fastutil.bytes.Byte2ShortOpenCustomHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ShortOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ShortRBTreeMap;
import it.unimi.dsi.fastutil.bytes.Byte2ShortSortedMap;
import it.unimi.dsi.fastutil.bytes.Byte2ShortSortedMaps;
import it.unimi.dsi.fastutil.bytes.ByteAVLTreeSet;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import it.unimi.dsi.fastutil.bytes.ByteBigArrayBigList;
import it.unimi.dsi.fastutil.bytes.ByteBigList;
import it.unimi.dsi.fastutil.bytes.ByteBigLists;
import it.unimi.dsi.fastutil.bytes.ByteCollection;
import it.unimi.dsi.fastutil.bytes.ByteCollections;
import it.unimi.dsi.fastutil.bytes.ByteImmutableList;
import it.unimi.dsi.fastutil.bytes.ByteLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.bytes.ByteLinkedOpenHashSet;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.bytes.ByteLists;
import it.unimi.dsi.fastutil.bytes.ByteMappedBigList;
import it.unimi.dsi.fastutil.bytes.ByteOpenCustomHashSet;
import it.unimi.dsi.fastutil.bytes.ByteOpenHashSet;
import it.unimi.dsi.fastutil.bytes.ByteRBTreeSet;
import it.unimi.dsi.fastutil.bytes.ByteSet;
import it.unimi.dsi.fastutil.bytes.ByteSets;
import it.unimi.dsi.fastutil.bytes.ByteSortedSet;
import it.unimi.dsi.fastutil.bytes.ByteSortedSets;
import it.unimi.dsi.fastutil.chars.Char2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2BooleanArrayMap;
import it.unimi.dsi.fastutil.chars.Char2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2BooleanMap;
import it.unimi.dsi.fastutil.chars.Char2BooleanMaps;
import it.unimi.dsi.fastutil.chars.Char2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.chars.Char2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2BooleanRBTreeMap;
import it.unimi.dsi.fastutil.chars.Char2BooleanSortedMap;
import it.unimi.dsi.fastutil.chars.Char2BooleanSortedMaps;
import it.unimi.dsi.fastutil.chars.Char2ByteAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2ByteArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2ByteMap;
import it.unimi.dsi.fastutil.chars.Char2ByteMaps;
import it.unimi.dsi.fastutil.chars.Char2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.chars.Char2ByteOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2ByteRBTreeMap;
import it.unimi.dsi.fastutil.chars.Char2ByteSortedMap;
import it.unimi.dsi.fastutil.chars.Char2ByteSortedMaps;
import it.unimi.dsi.fastutil.chars.Char2CharAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharLinkedOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import it.unimi.dsi.fastutil.chars.Char2CharMaps;
import it.unimi.dsi.fastutil.chars.Char2CharOpenCustomHashMap;
import it.unimi.dsi.fastutil.chars.Char2CharOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2CharRBTreeMap;
import it.unimi.dsi.fastutil.chars.Char2CharSortedMap;
import it.unimi.dsi.fastutil.chars.Char2CharSortedMaps;
import it.unimi.dsi.fastutil.chars.Char2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2DoubleArrayMap;
import it.unimi.dsi.fastutil.chars.Char2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2DoubleMap;
import it.unimi.dsi.fastutil.chars.Char2DoubleMaps;
import it.unimi.dsi.fastutil.chars.Char2DoubleOpenCustomHashMap;
import it.unimi.dsi.fastutil.chars.Char2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.chars.Char2DoubleSortedMap;
import it.unimi.dsi.fastutil.chars.Char2DoubleSortedMaps;
import it.unimi.dsi.fastutil.chars.Char2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2FloatArrayMap;
import it.unimi.dsi.fastutil.chars.Char2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2FloatMap;
import it.unimi.dsi.fastutil.chars.Char2FloatMaps;
import it.unimi.dsi.fastutil.chars.Char2FloatOpenCustomHashMap;
import it.unimi.dsi.fastutil.chars.Char2FloatOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2FloatRBTreeMap;
import it.unimi.dsi.fastutil.chars.Char2FloatSortedMap;
import it.unimi.dsi.fastutil.chars.Char2FloatSortedMaps;
import it.unimi.dsi.fastutil.chars.Char2IntAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import it.unimi.dsi.fastutil.chars.Char2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2IntMap;
import it.unimi.dsi.fastutil.chars.Char2IntMaps;
import it.unimi.dsi.fastutil.chars.Char2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2IntRBTreeMap;
import it.unimi.dsi.fastutil.chars.Char2IntSortedMap;
import it.unimi.dsi.fastutil.chars.Char2IntSortedMaps;
import it.unimi.dsi.fastutil.chars.Char2LongAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2LongArrayMap;
import it.unimi.dsi.fastutil.chars.Char2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2LongMap;
import it.unimi.dsi.fastutil.chars.Char2LongMaps;
import it.unimi.dsi.fastutil.chars.Char2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.chars.Char2LongOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2LongRBTreeMap;
import it.unimi.dsi.fastutil.chars.Char2LongSortedMap;
import it.unimi.dsi.fastutil.chars.Char2LongSortedMaps;
import it.unimi.dsi.fastutil.chars.Char2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMaps;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectSortedMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectSortedMaps;
import it.unimi.dsi.fastutil.chars.Char2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2ReferenceArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2ReferenceMap;
import it.unimi.dsi.fastutil.chars.Char2ReferenceMaps;
import it.unimi.dsi.fastutil.chars.Char2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.chars.Char2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.chars.Char2ReferenceSortedMap;
import it.unimi.dsi.fastutil.chars.Char2ReferenceSortedMaps;
import it.unimi.dsi.fastutil.chars.Char2ShortAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2ShortArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2ShortMap;
import it.unimi.dsi.fastutil.chars.Char2ShortMaps;
import it.unimi.dsi.fastutil.chars.Char2ShortOpenCustomHashMap;
import it.unimi.dsi.fastutil.chars.Char2ShortOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2ShortRBTreeMap;
import it.unimi.dsi.fastutil.chars.Char2ShortSortedMap;
import it.unimi.dsi.fastutil.chars.Char2ShortSortedMaps;
import it.unimi.dsi.fastutil.chars.CharAVLTreeSet;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharBigArrayBigList;
import it.unimi.dsi.fastutil.chars.CharBigList;
import it.unimi.dsi.fastutil.chars.CharBigLists;
import it.unimi.dsi.fastutil.chars.CharCollection;
import it.unimi.dsi.fastutil.chars.CharCollections;
import it.unimi.dsi.fastutil.chars.CharImmutableList;
import it.unimi.dsi.fastutil.chars.CharLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.chars.CharLinkedOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharList;
import it.unimi.dsi.fastutil.chars.CharLists;
import it.unimi.dsi.fastutil.chars.CharMappedBigList;
import it.unimi.dsi.fastutil.chars.CharOpenCustomHashSet;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharRBTreeSet;
import it.unimi.dsi.fastutil.chars.CharSet;
import it.unimi.dsi.fastutil.chars.CharSets;
import it.unimi.dsi.fastutil.chars.CharSortedSet;
import it.unimi.dsi.fastutil.chars.CharSortedSets;
import it.unimi.dsi.fastutil.doubles.Double2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2BooleanArrayMap;
import it.unimi.dsi.fastutil.doubles.Double2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2BooleanMap;
import it.unimi.dsi.fastutil.doubles.Double2BooleanMaps;
import it.unimi.dsi.fastutil.doubles.Double2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.doubles.Double2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2BooleanRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2BooleanSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2BooleanSortedMaps;
import it.unimi.dsi.fastutil.doubles.Double2ByteAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ByteArrayMap;
import it.unimi.dsi.fastutil.doubles.Double2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ByteMap;
import it.unimi.dsi.fastutil.doubles.Double2ByteMaps;
import it.unimi.dsi.fastutil.doubles.Double2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ByteOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ByteRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ByteSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2ByteSortedMaps;
import it.unimi.dsi.fastutil.doubles.Double2CharAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2CharArrayMap;
import it.unimi.dsi.fastutil.doubles.Double2CharLinkedOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2CharMap;
import it.unimi.dsi.fastutil.doubles.Double2CharMaps;
import it.unimi.dsi.fastutil.doubles.Double2CharOpenCustomHashMap;
import it.unimi.dsi.fastutil.doubles.Double2CharOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2CharRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2CharSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2CharSortedMaps;
import it.unimi.dsi.fastutil.doubles.Double2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2DoubleArrayMap;
import it.unimi.dsi.fastutil.doubles.Double2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2DoubleMap;
import it.unimi.dsi.fastutil.doubles.Double2DoubleMaps;
import it.unimi.dsi.fastutil.doubles.Double2DoubleOpenCustomHashMap;
import it.unimi.dsi.fastutil.doubles.Double2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2DoubleSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2DoubleSortedMaps;
import it.unimi.dsi.fastutil.doubles.Double2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2FloatArrayMap;
import it.unimi.dsi.fastutil.doubles.Double2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2FloatMap;
import it.unimi.dsi.fastutil.doubles.Double2FloatMaps;
import it.unimi.dsi.fastutil.doubles.Double2FloatOpenCustomHashMap;
import it.unimi.dsi.fastutil.doubles.Double2FloatOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2FloatRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2FloatSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2FloatSortedMaps;
import it.unimi.dsi.fastutil.doubles.Double2IntAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2IntArrayMap;
import it.unimi.dsi.fastutil.doubles.Double2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2IntMap;
import it.unimi.dsi.fastutil.doubles.Double2IntMaps;
import it.unimi.dsi.fastutil.doubles.Double2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.doubles.Double2IntOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2IntRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2IntSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2IntSortedMaps;
import it.unimi.dsi.fastutil.doubles.Double2LongAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2LongArrayMap;
import it.unimi.dsi.fastutil.doubles.Double2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2LongMap;
import it.unimi.dsi.fastutil.doubles.Double2LongMaps;
import it.unimi.dsi.fastutil.doubles.Double2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.doubles.Double2LongOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2LongRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2LongSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2LongSortedMaps;
import it.unimi.dsi.fastutil.doubles.Double2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectArrayMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMaps;
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectSortedMaps;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceArrayMap;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceMap;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceMaps;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceSortedMaps;
import it.unimi.dsi.fastutil.doubles.Double2ShortAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ShortArrayMap;
import it.unimi.dsi.fastutil.doubles.Double2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ShortMap;
import it.unimi.dsi.fastutil.doubles.Double2ShortMaps;
import it.unimi.dsi.fastutil.doubles.Double2ShortOpenCustomHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ShortOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2ShortRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ShortSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2ShortSortedMaps;
import it.unimi.dsi.fastutil.doubles.DoubleAVLTreeSet;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArraySet;
import it.unimi.dsi.fastutil.doubles.DoubleBigArrayBigList;
import it.unimi.dsi.fastutil.doubles.DoubleBigList;
import it.unimi.dsi.fastutil.doubles.DoubleBigLists;
import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleCollections;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import it.unimi.dsi.fastutil.doubles.DoubleLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.doubles.DoubleLinkedOpenHashSet;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleLists;
import it.unimi.dsi.fastutil.doubles.DoubleMappedBigList;
import it.unimi.dsi.fastutil.doubles.DoubleOpenCustomHashSet;
import it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet;
import it.unimi.dsi.fastutil.doubles.DoubleRBTreeSet;
import it.unimi.dsi.fastutil.doubles.DoubleSet;
import it.unimi.dsi.fastutil.doubles.DoubleSets;
import it.unimi.dsi.fastutil.doubles.DoubleSortedSet;
import it.unimi.dsi.fastutil.doubles.DoubleSortedSets;
import it.unimi.dsi.fastutil.floats.Float2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2BooleanArrayMap;
import it.unimi.dsi.fastutil.floats.Float2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2BooleanMap;
import it.unimi.dsi.fastutil.floats.Float2BooleanMaps;
import it.unimi.dsi.fastutil.floats.Float2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.floats.Float2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2BooleanRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2BooleanSortedMap;
import it.unimi.dsi.fastutil.floats.Float2BooleanSortedMaps;
import it.unimi.dsi.fastutil.floats.Float2ByteAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ByteArrayMap;
import it.unimi.dsi.fastutil.floats.Float2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2ByteMap;
import it.unimi.dsi.fastutil.floats.Float2ByteMaps;
import it.unimi.dsi.fastutil.floats.Float2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.floats.Float2ByteOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2ByteRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ByteSortedMap;
import it.unimi.dsi.fastutil.floats.Float2ByteSortedMaps;
import it.unimi.dsi.fastutil.floats.Float2CharAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2CharArrayMap;
import it.unimi.dsi.fastutil.floats.Float2CharLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2CharMap;
import it.unimi.dsi.fastutil.floats.Float2CharMaps;
import it.unimi.dsi.fastutil.floats.Float2CharOpenCustomHashMap;
import it.unimi.dsi.fastutil.floats.Float2CharOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2CharRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2CharSortedMap;
import it.unimi.dsi.fastutil.floats.Float2CharSortedMaps;
import it.unimi.dsi.fastutil.floats.Float2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2DoubleArrayMap;
import it.unimi.dsi.fastutil.floats.Float2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2DoubleMap;
import it.unimi.dsi.fastutil.floats.Float2DoubleMaps;
import it.unimi.dsi.fastutil.floats.Float2DoubleOpenCustomHashMap;
import it.unimi.dsi.fastutil.floats.Float2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2DoubleSortedMap;
import it.unimi.dsi.fastutil.floats.Float2DoubleSortedMaps;
import it.unimi.dsi.fastutil.floats.Float2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2FloatArrayMap;
import it.unimi.dsi.fastutil.floats.Float2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2FloatMap;
import it.unimi.dsi.fastutil.floats.Float2FloatMaps;
import it.unimi.dsi.fastutil.floats.Float2FloatOpenCustomHashMap;
import it.unimi.dsi.fastutil.floats.Float2FloatOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2FloatRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2FloatSortedMap;
import it.unimi.dsi.fastutil.floats.Float2FloatSortedMaps;
import it.unimi.dsi.fastutil.floats.Float2IntAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2IntArrayMap;
import it.unimi.dsi.fastutil.floats.Float2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2IntMap;
import it.unimi.dsi.fastutil.floats.Float2IntMaps;
import it.unimi.dsi.fastutil.floats.Float2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.floats.Float2IntOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2IntRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2IntSortedMap;
import it.unimi.dsi.fastutil.floats.Float2IntSortedMaps;
import it.unimi.dsi.fastutil.floats.Float2LongAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2LongArrayMap;
import it.unimi.dsi.fastutil.floats.Float2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2LongMap;
import it.unimi.dsi.fastutil.floats.Float2LongMaps;
import it.unimi.dsi.fastutil.floats.Float2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.floats.Float2LongOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2LongRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2LongSortedMap;
import it.unimi.dsi.fastutil.floats.Float2LongSortedMaps;
import it.unimi.dsi.fastutil.floats.Float2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectArrayMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectMaps;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectSortedMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectSortedMaps;
import it.unimi.dsi.fastutil.floats.Float2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ReferenceArrayMap;
import it.unimi.dsi.fastutil.floats.Float2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2ReferenceMap;
import it.unimi.dsi.fastutil.floats.Float2ReferenceMaps;
import it.unimi.dsi.fastutil.floats.Float2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.floats.Float2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ReferenceSortedMap;
import it.unimi.dsi.fastutil.floats.Float2ReferenceSortedMaps;
import it.unimi.dsi.fastutil.floats.Float2ShortAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ShortArrayMap;
import it.unimi.dsi.fastutil.floats.Float2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2ShortMap;
import it.unimi.dsi.fastutil.floats.Float2ShortMaps;
import it.unimi.dsi.fastutil.floats.Float2ShortOpenCustomHashMap;
import it.unimi.dsi.fastutil.floats.Float2ShortOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2ShortRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ShortSortedMap;
import it.unimi.dsi.fastutil.floats.Float2ShortSortedMaps;
import it.unimi.dsi.fastutil.floats.FloatAVLTreeSet;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatBigArrayBigList;
import it.unimi.dsi.fastutil.floats.FloatBigList;
import it.unimi.dsi.fastutil.floats.FloatBigLists;
import it.unimi.dsi.fastutil.floats.FloatCollection;
import it.unimi.dsi.fastutil.floats.FloatCollections;
import it.unimi.dsi.fastutil.floats.FloatImmutableList;
import it.unimi.dsi.fastutil.floats.FloatLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.floats.FloatLinkedOpenHashSet;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.floats.FloatLists;
import it.unimi.dsi.fastutil.floats.FloatMappedBigList;
import it.unimi.dsi.fastutil.floats.FloatOpenCustomHashSet;
import it.unimi.dsi.fastutil.floats.FloatOpenHashSet;
import it.unimi.dsi.fastutil.floats.FloatRBTreeSet;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.floats.FloatSets;
import it.unimi.dsi.fastutil.floats.FloatSortedSet;
import it.unimi.dsi.fastutil.floats.FloatSortedSets;
import it.unimi.dsi.fastutil.ints.Int2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMaps;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanSortedMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanSortedMaps;
import it.unimi.dsi.fastutil.ints.Int2ByteAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ByteArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ByteMap;
import it.unimi.dsi.fastutil.ints.Int2ByteMaps;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ByteRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ByteSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ByteSortedMaps;
import it.unimi.dsi.fastutil.ints.Int2CharAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2CharArrayMap;
import it.unimi.dsi.fastutil.ints.Int2CharLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2CharMap;
import it.unimi.dsi.fastutil.ints.Int2CharMaps;
import it.unimi.dsi.fastutil.ints.Int2CharOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2CharOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2CharRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2CharSortedMap;
import it.unimi.dsi.fastutil.ints.Int2CharSortedMaps;
import it.unimi.dsi.fastutil.ints.Int2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMaps;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleSortedMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleSortedMaps;
import it.unimi.dsi.fastutil.ints.Int2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2FloatArrayMap;
import it.unimi.dsi.fastutil.ints.Int2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMaps;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2FloatSortedMap;
import it.unimi.dsi.fastutil.ints.Int2FloatSortedMaps;
import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMap;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMaps;
import it.unimi.dsi.fastutil.ints.Int2LongAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongMaps;
import it.unimi.dsi.fastutil.ints.Int2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2LongSortedMap;
import it.unimi.dsi.fastutil.ints.Int2LongSortedMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMaps;
import it.unimi.dsi.fastutil.ints.Int2ShortAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ShortArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ShortMap;
import it.unimi.dsi.fastutil.ints.Int2ShortMaps;
import it.unimi.dsi.fastutil.ints.Int2ShortOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ShortRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ShortSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ShortSortedMaps;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntBigArrayBigList;
import it.unimi.dsi.fastutil.ints.IntBigList;
import it.unimi.dsi.fastutil.ints.IntBigLists;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntCollections;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.ints.IntMappedBigList;
import it.unimi.dsi.fastutil.ints.IntOpenCustomHashSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.ints.IntSortedSets;
import it.unimi.dsi.fastutil.longs.Long2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanArrayMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanMaps;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanSortedMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanSortedMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ByteArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ByteSortedMap;
import it.unimi.dsi.fastutil.longs.Long2ByteSortedMaps;
import it.unimi.dsi.fastutil.longs.Long2CharAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2CharArrayMap;
import it.unimi.dsi.fastutil.longs.Long2CharLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2CharMap;
import it.unimi.dsi.fastutil.longs.Long2CharMaps;
import it.unimi.dsi.fastutil.longs.Long2CharOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.Long2CharOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2CharRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2CharSortedMap;
import it.unimi.dsi.fastutil.longs.Long2CharSortedMaps;
import it.unimi.dsi.fastutil.longs.Long2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleArrayMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMaps;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleSortedMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleSortedMaps;
import it.unimi.dsi.fastutil.longs.Long2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2FloatArrayMap;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMaps;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2FloatSortedMap;
import it.unimi.dsi.fastutil.longs.Long2FloatSortedMaps;
import it.unimi.dsi.fastutil.longs.Long2IntAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2IntArrayMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2IntSortedMap;
import it.unimi.dsi.fastutil.longs.Long2IntSortedMaps;
import it.unimi.dsi.fastutil.longs.Long2LongAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2LongArrayMap;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2LongSortedMap;
import it.unimi.dsi.fastutil.longs.Long2LongSortedMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMaps;
import it.unimi.dsi.fastutil.longs.Long2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMaps;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceSortedMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceSortedMaps;
import it.unimi.dsi.fastutil.longs.Long2ShortAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ShortArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ShortMap;
import it.unimi.dsi.fastutil.longs.Long2ShortMaps;
import it.unimi.dsi.fastutil.longs.Long2ShortOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ShortRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ShortSortedMap;
import it.unimi.dsi.fastutil.longs.Long2ShortSortedMaps;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import it.unimi.dsi.fastutil.longs.LongBigList;
import it.unimi.dsi.fastutil.longs.LongBigLists;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongCollections;
import it.unimi.dsi.fastutil.longs.LongImmutableList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import it.unimi.dsi.fastutil.longs.LongMappedBigList;
import it.unimi.dsi.fastutil.longs.LongOpenCustomHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import it.unimi.dsi.fastutil.longs.LongSortedSets;
import it.unimi.dsi.fastutil.objects.Object2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanSortedMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanSortedMaps;
import it.unimi.dsi.fastutil.objects.Object2ByteAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMaps;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ByteRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ByteSortedMap;
import it.unimi.dsi.fastutil.objects.Object2ByteSortedMaps;
import it.unimi.dsi.fastutil.objects.Object2CharAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2CharArrayMap;
import it.unimi.dsi.fastutil.objects.Object2CharLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2CharLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2CharMap;
import it.unimi.dsi.fastutil.objects.Object2CharMaps;
import it.unimi.dsi.fastutil.objects.Object2CharOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2CharOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2CharRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2CharSortedMap;
import it.unimi.dsi.fastutil.objects.Object2CharSortedMaps;
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMaps;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleSortedMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleSortedMaps;
import it.unimi.dsi.fastutil.objects.Object2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.Object2FloatLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMaps;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;
import it.unimi.dsi.fastutil.objects.Object2FloatSortedMaps;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMaps;
import it.unimi.dsi.fastutil.objects.Object2LongAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceSortedMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceSortedMaps;
import it.unimi.dsi.fastutil.objects.Object2ShortAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ShortArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ShortLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;
import it.unimi.dsi.fastutil.objects.Object2ShortMaps;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ShortSortedMap;
import it.unimi.dsi.fastutil.objects.Object2ShortSortedMaps;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import it.unimi.dsi.fastutil.objects.ObjectBigList;
import it.unimi.dsi.fastutil.objects.ObjectBigLists;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectCollections;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSets;
import it.unimi.dsi.fastutil.objects.Reference2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanSortedMaps;
import it.unimi.dsi.fastutil.objects.Reference2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ByteMap;
import it.unimi.dsi.fastutil.objects.Reference2ByteMaps;
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ByteSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2ByteSortedMaps;
import it.unimi.dsi.fastutil.objects.Reference2CharArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2CharLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2CharMap;
import it.unimi.dsi.fastutil.objects.Reference2CharMaps;
import it.unimi.dsi.fastutil.objects.Reference2CharOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2CharOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2CharSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2CharSortedMaps;
import it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMaps;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleSortedMaps;
import it.unimi.dsi.fastutil.objects.Reference2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatMaps;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatSortedMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2IntSortedMaps;
import it.unimi.dsi.fastutil.objects.Reference2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongMaps;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2LongSortedMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectSortedMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceSortedMaps;
import it.unimi.dsi.fastutil.objects.Reference2ShortArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ShortMap;
import it.unimi.dsi.fastutil.objects.Reference2ShortMaps;
import it.unimi.dsi.fastutil.objects.Reference2ShortOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ShortSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2ShortSortedMaps;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceBigArrayBigList;
import it.unimi.dsi.fastutil.objects.ReferenceBigList;
import it.unimi.dsi.fastutil.objects.ReferenceBigLists;
import it.unimi.dsi.fastutil.objects.ReferenceCollection;
import it.unimi.dsi.fastutil.objects.ReferenceCollections;
import it.unimi.dsi.fastutil.objects.ReferenceImmutableList;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import it.unimi.dsi.fastutil.objects.ReferenceLists;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSet;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import it.unimi.dsi.fastutil.shorts.Short2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMaps;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanSortedMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanSortedMaps;
import it.unimi.dsi.fastutil.shorts.Short2ByteAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ByteArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ByteMap;
import it.unimi.dsi.fastutil.shorts.Short2ByteMaps;
import it.unimi.dsi.fastutil.shorts.Short2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ByteOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ByteRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ByteSortedMap;
import it.unimi.dsi.fastutil.shorts.Short2ByteSortedMaps;
import it.unimi.dsi.fastutil.shorts.Short2CharAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2CharArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2CharLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2CharMap;
import it.unimi.dsi.fastutil.shorts.Short2CharMaps;
import it.unimi.dsi.fastutil.shorts.Short2CharOpenCustomHashMap;
import it.unimi.dsi.fastutil.shorts.Short2CharOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2CharRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2CharSortedMap;
import it.unimi.dsi.fastutil.shorts.Short2CharSortedMaps;
import it.unimi.dsi.fastutil.shorts.Short2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2DoubleArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2DoubleMap;
import it.unimi.dsi.fastutil.shorts.Short2DoubleMaps;
import it.unimi.dsi.fastutil.shorts.Short2DoubleOpenCustomHashMap;
import it.unimi.dsi.fastutil.shorts.Short2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2DoubleSortedMap;
import it.unimi.dsi.fastutil.shorts.Short2DoubleSortedMaps;
import it.unimi.dsi.fastutil.shorts.Short2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatMaps;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenCustomHashMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatSortedMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatSortedMaps;
import it.unimi.dsi.fastutil.shorts.Short2IntAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2IntArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2IntMaps;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2IntRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2IntSortedMap;
import it.unimi.dsi.fastutil.shorts.Short2IntSortedMaps;
import it.unimi.dsi.fastutil.shorts.Short2LongAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2LongArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2LongMap;
import it.unimi.dsi.fastutil.shorts.Short2LongMaps;
import it.unimi.dsi.fastutil.shorts.Short2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.shorts.Short2LongOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2LongRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2LongSortedMap;
import it.unimi.dsi.fastutil.shorts.Short2LongSortedMaps;
import it.unimi.dsi.fastutil.shorts.Short2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMaps;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectSortedMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectSortedMaps;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceMaps;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceSortedMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceSortedMaps;
import it.unimi.dsi.fastutil.shorts.Short2ShortAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortMaps;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenCustomHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortSortedMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortSortedMaps;
import it.unimi.dsi.fastutil.shorts.ShortAVLTreeSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import it.unimi.dsi.fastutil.shorts.ShortBigArrayBigList;
import it.unimi.dsi.fastutil.shorts.ShortBigList;
import it.unimi.dsi.fastutil.shorts.ShortBigLists;
import it.unimi.dsi.fastutil.shorts.ShortCollection;
import it.unimi.dsi.fastutil.shorts.ShortCollections;
import it.unimi.dsi.fastutil.shorts.ShortImmutableList;
import it.unimi.dsi.fastutil.shorts.ShortLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.shorts.ShortLinkedOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortLists;
import it.unimi.dsi.fastutil.shorts.ShortMappedBigList;
import it.unimi.dsi.fastutil.shorts.ShortOpenCustomHashSet;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortRBTreeSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import it.unimi.dsi.fastutil.shorts.ShortSets;
import it.unimi.dsi.fastutil.shorts.ShortSortedSet;
import it.unimi.dsi.fastutil.shorts.ShortSortedSets;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/** Explicit fastutil factories; unchecked casts stay at this erased container boundary. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class FastutilCollections {

    private static final Map<Class<?>, BiFunction<Type, Object, Construction>> FACTORIES = new Object2ObjectOpenHashMap<>();
    private static final Set<Class<?>> READ_ONLY = new ObjectOpenHashSet<>();

    static {
        registerBoolean();
        registerByte();
        registerShort();
        registerChar();
        registerInt();
        registerLong();
        registerFloat();
        registerDouble();
        registerObject();
        registerReference();
        READ_ONLY.add(BooleanCollections.unmodifiable(new BooleanArrayList()).getClass());
        READ_ONLY.add(BooleanLists.UnmodifiableList.class);
        READ_ONLY.add(BooleanLists.EmptyList.class);
        READ_ONLY.add(BooleanLists.Singleton.class);
        READ_ONLY.add(BooleanLists.UnmodifiableRandomAccessList.class);
        READ_ONLY.add(BooleanSets.UnmodifiableSet.class);
        READ_ONLY.add(BooleanSets.EmptySet.class);
        READ_ONLY.add(BooleanSets.Singleton.class);
        READ_ONLY.add(BooleanBigLists.UnmodifiableBigList.class);
        READ_ONLY.add(BooleanBigLists.EmptyBigList.class);
        READ_ONLY.add(BooleanBigLists.Singleton.class);
        READ_ONLY.add(BooleanImmutableList.class);
        READ_ONLY.add(new BooleanImmutableList(new boolean[] { false, true }).subList(0, 1).getClass());
        READ_ONLY.add(Byte2BooleanSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Byte2BooleanSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Byte2BooleanSortedMaps.Singleton.class);
        READ_ONLY.add(Byte2BooleanMaps.UnmodifiableMap.class);
        READ_ONLY.add(Byte2BooleanMaps.EmptyMap.class);
        READ_ONLY.add(Byte2BooleanMaps.Singleton.class);
        READ_ONLY.add(Byte2ByteSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Byte2ByteSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Byte2ByteSortedMaps.Singleton.class);
        READ_ONLY.add(Byte2ByteMaps.UnmodifiableMap.class);
        READ_ONLY.add(Byte2ByteMaps.EmptyMap.class);
        READ_ONLY.add(Byte2ByteMaps.Singleton.class);
        READ_ONLY.add(Byte2ShortSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Byte2ShortSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Byte2ShortSortedMaps.Singleton.class);
        READ_ONLY.add(Byte2ShortMaps.UnmodifiableMap.class);
        READ_ONLY.add(Byte2ShortMaps.EmptyMap.class);
        READ_ONLY.add(Byte2ShortMaps.Singleton.class);
        READ_ONLY.add(Byte2CharSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Byte2CharSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Byte2CharSortedMaps.Singleton.class);
        READ_ONLY.add(Byte2CharMaps.UnmodifiableMap.class);
        READ_ONLY.add(Byte2CharMaps.EmptyMap.class);
        READ_ONLY.add(Byte2CharMaps.Singleton.class);
        READ_ONLY.add(Byte2IntSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Byte2IntSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Byte2IntSortedMaps.Singleton.class);
        READ_ONLY.add(Byte2IntMaps.UnmodifiableMap.class);
        READ_ONLY.add(Byte2IntMaps.EmptyMap.class);
        READ_ONLY.add(Byte2IntMaps.Singleton.class);
        READ_ONLY.add(Byte2LongSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Byte2LongSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Byte2LongSortedMaps.Singleton.class);
        READ_ONLY.add(Byte2LongMaps.UnmodifiableMap.class);
        READ_ONLY.add(Byte2LongMaps.EmptyMap.class);
        READ_ONLY.add(Byte2LongMaps.Singleton.class);
        READ_ONLY.add(Byte2FloatSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Byte2FloatSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Byte2FloatSortedMaps.Singleton.class);
        READ_ONLY.add(Byte2FloatMaps.UnmodifiableMap.class);
        READ_ONLY.add(Byte2FloatMaps.EmptyMap.class);
        READ_ONLY.add(Byte2FloatMaps.Singleton.class);
        READ_ONLY.add(Byte2DoubleSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Byte2DoubleSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Byte2DoubleSortedMaps.Singleton.class);
        READ_ONLY.add(Byte2DoubleMaps.UnmodifiableMap.class);
        READ_ONLY.add(Byte2DoubleMaps.EmptyMap.class);
        READ_ONLY.add(Byte2DoubleMaps.Singleton.class);
        READ_ONLY.add(Byte2ObjectSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Byte2ObjectSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Byte2ObjectSortedMaps.Singleton.class);
        READ_ONLY.add(Byte2ObjectMaps.UnmodifiableMap.class);
        READ_ONLY.add(Byte2ObjectMaps.EmptyMap.class);
        READ_ONLY.add(Byte2ObjectMaps.Singleton.class);
        READ_ONLY.add(Byte2ReferenceSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Byte2ReferenceSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Byte2ReferenceSortedMaps.Singleton.class);
        READ_ONLY.add(Byte2ReferenceMaps.UnmodifiableMap.class);
        READ_ONLY.add(Byte2ReferenceMaps.EmptyMap.class);
        READ_ONLY.add(Byte2ReferenceMaps.Singleton.class);
        READ_ONLY.add(ByteCollections.unmodifiable(new ByteArrayList()).getClass());
        READ_ONLY.add(ByteLists.UnmodifiableList.class);
        READ_ONLY.add(ByteLists.EmptyList.class);
        READ_ONLY.add(ByteLists.Singleton.class);
        READ_ONLY.add(ByteLists.UnmodifiableRandomAccessList.class);
        READ_ONLY.add(ByteSets.UnmodifiableSet.class);
        READ_ONLY.add(ByteSets.EmptySet.class);
        READ_ONLY.add(ByteSets.Singleton.class);
        READ_ONLY.add(ByteSortedSets.UnmodifiableSortedSet.class);
        READ_ONLY.add(ByteSortedSets.EmptySet.class);
        READ_ONLY.add(ByteSortedSets.Singleton.class);
        READ_ONLY.add(ByteBigLists.UnmodifiableBigList.class);
        READ_ONLY.add(ByteBigLists.EmptyBigList.class);
        READ_ONLY.add(ByteBigLists.Singleton.class);
        READ_ONLY.add(ByteImmutableList.class);
        READ_ONLY.add(new ByteImmutableList(new byte[] { 0, 1 }).subList(0, 1).getClass());
        READ_ONLY.add(Short2BooleanSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Short2BooleanSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Short2BooleanSortedMaps.Singleton.class);
        READ_ONLY.add(Short2BooleanMaps.UnmodifiableMap.class);
        READ_ONLY.add(Short2BooleanMaps.EmptyMap.class);
        READ_ONLY.add(Short2BooleanMaps.Singleton.class);
        READ_ONLY.add(Short2ByteSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Short2ByteSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Short2ByteSortedMaps.Singleton.class);
        READ_ONLY.add(Short2ByteMaps.UnmodifiableMap.class);
        READ_ONLY.add(Short2ByteMaps.EmptyMap.class);
        READ_ONLY.add(Short2ByteMaps.Singleton.class);
        READ_ONLY.add(Short2ShortSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Short2ShortSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Short2ShortSortedMaps.Singleton.class);
        READ_ONLY.add(Short2ShortMaps.UnmodifiableMap.class);
        READ_ONLY.add(Short2ShortMaps.EmptyMap.class);
        READ_ONLY.add(Short2ShortMaps.Singleton.class);
        READ_ONLY.add(Short2CharSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Short2CharSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Short2CharSortedMaps.Singleton.class);
        READ_ONLY.add(Short2CharMaps.UnmodifiableMap.class);
        READ_ONLY.add(Short2CharMaps.EmptyMap.class);
        READ_ONLY.add(Short2CharMaps.Singleton.class);
        READ_ONLY.add(Short2IntSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Short2IntSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Short2IntSortedMaps.Singleton.class);
        READ_ONLY.add(Short2IntMaps.UnmodifiableMap.class);
        READ_ONLY.add(Short2IntMaps.EmptyMap.class);
        READ_ONLY.add(Short2IntMaps.Singleton.class);
        READ_ONLY.add(Short2LongSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Short2LongSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Short2LongSortedMaps.Singleton.class);
        READ_ONLY.add(Short2LongMaps.UnmodifiableMap.class);
        READ_ONLY.add(Short2LongMaps.EmptyMap.class);
        READ_ONLY.add(Short2LongMaps.Singleton.class);
        READ_ONLY.add(Short2FloatSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Short2FloatSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Short2FloatSortedMaps.Singleton.class);
        READ_ONLY.add(Short2FloatMaps.UnmodifiableMap.class);
        READ_ONLY.add(Short2FloatMaps.EmptyMap.class);
        READ_ONLY.add(Short2FloatMaps.Singleton.class);
        READ_ONLY.add(Short2DoubleSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Short2DoubleSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Short2DoubleSortedMaps.Singleton.class);
        READ_ONLY.add(Short2DoubleMaps.UnmodifiableMap.class);
        READ_ONLY.add(Short2DoubleMaps.EmptyMap.class);
        READ_ONLY.add(Short2DoubleMaps.Singleton.class);
        READ_ONLY.add(Short2ObjectSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Short2ObjectSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Short2ObjectSortedMaps.Singleton.class);
        READ_ONLY.add(Short2ObjectMaps.UnmodifiableMap.class);
        READ_ONLY.add(Short2ObjectMaps.EmptyMap.class);
        READ_ONLY.add(Short2ObjectMaps.Singleton.class);
        READ_ONLY.add(Short2ReferenceSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Short2ReferenceSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Short2ReferenceSortedMaps.Singleton.class);
        READ_ONLY.add(Short2ReferenceMaps.UnmodifiableMap.class);
        READ_ONLY.add(Short2ReferenceMaps.EmptyMap.class);
        READ_ONLY.add(Short2ReferenceMaps.Singleton.class);
        READ_ONLY.add(ShortCollections.unmodifiable(new ShortArrayList()).getClass());
        READ_ONLY.add(ShortLists.UnmodifiableList.class);
        READ_ONLY.add(ShortLists.EmptyList.class);
        READ_ONLY.add(ShortLists.Singleton.class);
        READ_ONLY.add(ShortLists.UnmodifiableRandomAccessList.class);
        READ_ONLY.add(ShortSets.UnmodifiableSet.class);
        READ_ONLY.add(ShortSets.EmptySet.class);
        READ_ONLY.add(ShortSets.Singleton.class);
        READ_ONLY.add(ShortSortedSets.UnmodifiableSortedSet.class);
        READ_ONLY.add(ShortSortedSets.EmptySet.class);
        READ_ONLY.add(ShortSortedSets.Singleton.class);
        READ_ONLY.add(ShortBigLists.UnmodifiableBigList.class);
        READ_ONLY.add(ShortBigLists.EmptyBigList.class);
        READ_ONLY.add(ShortBigLists.Singleton.class);
        READ_ONLY.add(ShortImmutableList.class);
        READ_ONLY.add(new ShortImmutableList(new short[] { 0, 1 }).subList(0, 1).getClass());
        READ_ONLY.add(Char2BooleanSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Char2BooleanSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Char2BooleanSortedMaps.Singleton.class);
        READ_ONLY.add(Char2BooleanMaps.UnmodifiableMap.class);
        READ_ONLY.add(Char2BooleanMaps.EmptyMap.class);
        READ_ONLY.add(Char2BooleanMaps.Singleton.class);
        READ_ONLY.add(Char2ByteSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Char2ByteSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Char2ByteSortedMaps.Singleton.class);
        READ_ONLY.add(Char2ByteMaps.UnmodifiableMap.class);
        READ_ONLY.add(Char2ByteMaps.EmptyMap.class);
        READ_ONLY.add(Char2ByteMaps.Singleton.class);
        READ_ONLY.add(Char2ShortSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Char2ShortSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Char2ShortSortedMaps.Singleton.class);
        READ_ONLY.add(Char2ShortMaps.UnmodifiableMap.class);
        READ_ONLY.add(Char2ShortMaps.EmptyMap.class);
        READ_ONLY.add(Char2ShortMaps.Singleton.class);
        READ_ONLY.add(Char2CharSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Char2CharSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Char2CharSortedMaps.Singleton.class);
        READ_ONLY.add(Char2CharMaps.UnmodifiableMap.class);
        READ_ONLY.add(Char2CharMaps.EmptyMap.class);
        READ_ONLY.add(Char2CharMaps.Singleton.class);
        READ_ONLY.add(Char2IntSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Char2IntSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Char2IntSortedMaps.Singleton.class);
        READ_ONLY.add(Char2IntMaps.UnmodifiableMap.class);
        READ_ONLY.add(Char2IntMaps.EmptyMap.class);
        READ_ONLY.add(Char2IntMaps.Singleton.class);
        READ_ONLY.add(Char2LongSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Char2LongSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Char2LongSortedMaps.Singleton.class);
        READ_ONLY.add(Char2LongMaps.UnmodifiableMap.class);
        READ_ONLY.add(Char2LongMaps.EmptyMap.class);
        READ_ONLY.add(Char2LongMaps.Singleton.class);
        READ_ONLY.add(Char2FloatSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Char2FloatSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Char2FloatSortedMaps.Singleton.class);
        READ_ONLY.add(Char2FloatMaps.UnmodifiableMap.class);
        READ_ONLY.add(Char2FloatMaps.EmptyMap.class);
        READ_ONLY.add(Char2FloatMaps.Singleton.class);
        READ_ONLY.add(Char2DoubleSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Char2DoubleSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Char2DoubleSortedMaps.Singleton.class);
        READ_ONLY.add(Char2DoubleMaps.UnmodifiableMap.class);
        READ_ONLY.add(Char2DoubleMaps.EmptyMap.class);
        READ_ONLY.add(Char2DoubleMaps.Singleton.class);
        READ_ONLY.add(Char2ObjectSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Char2ObjectSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Char2ObjectSortedMaps.Singleton.class);
        READ_ONLY.add(Char2ObjectMaps.UnmodifiableMap.class);
        READ_ONLY.add(Char2ObjectMaps.EmptyMap.class);
        READ_ONLY.add(Char2ObjectMaps.Singleton.class);
        READ_ONLY.add(Char2ReferenceSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Char2ReferenceSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Char2ReferenceSortedMaps.Singleton.class);
        READ_ONLY.add(Char2ReferenceMaps.UnmodifiableMap.class);
        READ_ONLY.add(Char2ReferenceMaps.EmptyMap.class);
        READ_ONLY.add(Char2ReferenceMaps.Singleton.class);
        READ_ONLY.add(CharCollections.unmodifiable(new CharArrayList()).getClass());
        READ_ONLY.add(CharLists.UnmodifiableList.class);
        READ_ONLY.add(CharLists.EmptyList.class);
        READ_ONLY.add(CharLists.Singleton.class);
        READ_ONLY.add(CharLists.UnmodifiableRandomAccessList.class);
        READ_ONLY.add(CharSets.UnmodifiableSet.class);
        READ_ONLY.add(CharSets.EmptySet.class);
        READ_ONLY.add(CharSets.Singleton.class);
        READ_ONLY.add(CharSortedSets.UnmodifiableSortedSet.class);
        READ_ONLY.add(CharSortedSets.EmptySet.class);
        READ_ONLY.add(CharSortedSets.Singleton.class);
        READ_ONLY.add(CharBigLists.UnmodifiableBigList.class);
        READ_ONLY.add(CharBigLists.EmptyBigList.class);
        READ_ONLY.add(CharBigLists.Singleton.class);
        READ_ONLY.add(CharImmutableList.class);
        READ_ONLY.add(new CharImmutableList(new char[] { 0, 1 }).subList(0, 1).getClass());
        READ_ONLY.add(Int2BooleanSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Int2BooleanSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Int2BooleanSortedMaps.Singleton.class);
        READ_ONLY.add(Int2BooleanMaps.UnmodifiableMap.class);
        READ_ONLY.add(Int2BooleanMaps.EmptyMap.class);
        READ_ONLY.add(Int2BooleanMaps.Singleton.class);
        READ_ONLY.add(Int2ByteSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Int2ByteSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Int2ByteSortedMaps.Singleton.class);
        READ_ONLY.add(Int2ByteMaps.UnmodifiableMap.class);
        READ_ONLY.add(Int2ByteMaps.EmptyMap.class);
        READ_ONLY.add(Int2ByteMaps.Singleton.class);
        READ_ONLY.add(Int2ShortSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Int2ShortSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Int2ShortSortedMaps.Singleton.class);
        READ_ONLY.add(Int2ShortMaps.UnmodifiableMap.class);
        READ_ONLY.add(Int2ShortMaps.EmptyMap.class);
        READ_ONLY.add(Int2ShortMaps.Singleton.class);
        READ_ONLY.add(Int2CharSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Int2CharSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Int2CharSortedMaps.Singleton.class);
        READ_ONLY.add(Int2CharMaps.UnmodifiableMap.class);
        READ_ONLY.add(Int2CharMaps.EmptyMap.class);
        READ_ONLY.add(Int2CharMaps.Singleton.class);
        READ_ONLY.add(Int2IntSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Int2IntSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Int2IntSortedMaps.Singleton.class);
        READ_ONLY.add(Int2IntMaps.UnmodifiableMap.class);
        READ_ONLY.add(Int2IntMaps.EmptyMap.class);
        READ_ONLY.add(Int2IntMaps.Singleton.class);
        READ_ONLY.add(Int2LongSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Int2LongSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Int2LongSortedMaps.Singleton.class);
        READ_ONLY.add(Int2LongMaps.UnmodifiableMap.class);
        READ_ONLY.add(Int2LongMaps.EmptyMap.class);
        READ_ONLY.add(Int2LongMaps.Singleton.class);
        READ_ONLY.add(Int2FloatSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Int2FloatSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Int2FloatSortedMaps.Singleton.class);
        READ_ONLY.add(Int2FloatMaps.UnmodifiableMap.class);
        READ_ONLY.add(Int2FloatMaps.EmptyMap.class);
        READ_ONLY.add(Int2FloatMaps.Singleton.class);
        READ_ONLY.add(Int2DoubleSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Int2DoubleSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Int2DoubleSortedMaps.Singleton.class);
        READ_ONLY.add(Int2DoubleMaps.UnmodifiableMap.class);
        READ_ONLY.add(Int2DoubleMaps.EmptyMap.class);
        READ_ONLY.add(Int2DoubleMaps.Singleton.class);
        READ_ONLY.add(Int2ObjectSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Int2ObjectSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Int2ObjectSortedMaps.Singleton.class);
        READ_ONLY.add(Int2ObjectMaps.UnmodifiableMap.class);
        READ_ONLY.add(Int2ObjectMaps.EmptyMap.class);
        READ_ONLY.add(Int2ObjectMaps.Singleton.class);
        READ_ONLY.add(Int2ReferenceSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Int2ReferenceSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Int2ReferenceSortedMaps.Singleton.class);
        READ_ONLY.add(Int2ReferenceMaps.UnmodifiableMap.class);
        READ_ONLY.add(Int2ReferenceMaps.EmptyMap.class);
        READ_ONLY.add(Int2ReferenceMaps.Singleton.class);
        READ_ONLY.add(IntCollections.unmodifiable(new IntArrayList()).getClass());
        READ_ONLY.add(IntLists.UnmodifiableList.class);
        READ_ONLY.add(IntLists.EmptyList.class);
        READ_ONLY.add(IntLists.Singleton.class);
        READ_ONLY.add(IntLists.UnmodifiableRandomAccessList.class);
        READ_ONLY.add(IntSets.UnmodifiableSet.class);
        READ_ONLY.add(IntSets.EmptySet.class);
        READ_ONLY.add(IntSets.Singleton.class);
        READ_ONLY.add(IntSortedSets.UnmodifiableSortedSet.class);
        READ_ONLY.add(IntSortedSets.EmptySet.class);
        READ_ONLY.add(IntSortedSets.Singleton.class);
        READ_ONLY.add(IntBigLists.UnmodifiableBigList.class);
        READ_ONLY.add(IntBigLists.EmptyBigList.class);
        READ_ONLY.add(IntBigLists.Singleton.class);
        READ_ONLY.add(IntImmutableList.class);
        READ_ONLY.add(new IntImmutableList(new int[] { 0, 1 }).subList(0, 1).getClass());
        READ_ONLY.add(Long2BooleanSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Long2BooleanSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Long2BooleanSortedMaps.Singleton.class);
        READ_ONLY.add(Long2BooleanMaps.UnmodifiableMap.class);
        READ_ONLY.add(Long2BooleanMaps.EmptyMap.class);
        READ_ONLY.add(Long2BooleanMaps.Singleton.class);
        READ_ONLY.add(Long2ByteSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Long2ByteSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Long2ByteSortedMaps.Singleton.class);
        READ_ONLY.add(Long2ByteMaps.UnmodifiableMap.class);
        READ_ONLY.add(Long2ByteMaps.EmptyMap.class);
        READ_ONLY.add(Long2ByteMaps.Singleton.class);
        READ_ONLY.add(Long2ShortSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Long2ShortSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Long2ShortSortedMaps.Singleton.class);
        READ_ONLY.add(Long2ShortMaps.UnmodifiableMap.class);
        READ_ONLY.add(Long2ShortMaps.EmptyMap.class);
        READ_ONLY.add(Long2ShortMaps.Singleton.class);
        READ_ONLY.add(Long2CharSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Long2CharSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Long2CharSortedMaps.Singleton.class);
        READ_ONLY.add(Long2CharMaps.UnmodifiableMap.class);
        READ_ONLY.add(Long2CharMaps.EmptyMap.class);
        READ_ONLY.add(Long2CharMaps.Singleton.class);
        READ_ONLY.add(Long2IntSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Long2IntSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Long2IntSortedMaps.Singleton.class);
        READ_ONLY.add(Long2IntMaps.UnmodifiableMap.class);
        READ_ONLY.add(Long2IntMaps.EmptyMap.class);
        READ_ONLY.add(Long2IntMaps.Singleton.class);
        READ_ONLY.add(Long2LongSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Long2LongSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Long2LongSortedMaps.Singleton.class);
        READ_ONLY.add(Long2LongMaps.UnmodifiableMap.class);
        READ_ONLY.add(Long2LongMaps.EmptyMap.class);
        READ_ONLY.add(Long2LongMaps.Singleton.class);
        READ_ONLY.add(Long2FloatSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Long2FloatSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Long2FloatSortedMaps.Singleton.class);
        READ_ONLY.add(Long2FloatMaps.UnmodifiableMap.class);
        READ_ONLY.add(Long2FloatMaps.EmptyMap.class);
        READ_ONLY.add(Long2FloatMaps.Singleton.class);
        READ_ONLY.add(Long2DoubleSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Long2DoubleSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Long2DoubleSortedMaps.Singleton.class);
        READ_ONLY.add(Long2DoubleMaps.UnmodifiableMap.class);
        READ_ONLY.add(Long2DoubleMaps.EmptyMap.class);
        READ_ONLY.add(Long2DoubleMaps.Singleton.class);
        READ_ONLY.add(Long2ObjectSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Long2ObjectSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Long2ObjectSortedMaps.Singleton.class);
        READ_ONLY.add(Long2ObjectMaps.UnmodifiableMap.class);
        READ_ONLY.add(Long2ObjectMaps.EmptyMap.class);
        READ_ONLY.add(Long2ObjectMaps.Singleton.class);
        READ_ONLY.add(Long2ReferenceSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Long2ReferenceSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Long2ReferenceSortedMaps.Singleton.class);
        READ_ONLY.add(Long2ReferenceMaps.UnmodifiableMap.class);
        READ_ONLY.add(Long2ReferenceMaps.EmptyMap.class);
        READ_ONLY.add(Long2ReferenceMaps.Singleton.class);
        READ_ONLY.add(LongCollections.unmodifiable(new LongArrayList()).getClass());
        READ_ONLY.add(LongLists.UnmodifiableList.class);
        READ_ONLY.add(LongLists.EmptyList.class);
        READ_ONLY.add(LongLists.Singleton.class);
        READ_ONLY.add(LongLists.UnmodifiableRandomAccessList.class);
        READ_ONLY.add(LongSets.UnmodifiableSet.class);
        READ_ONLY.add(LongSets.EmptySet.class);
        READ_ONLY.add(LongSets.Singleton.class);
        READ_ONLY.add(LongSortedSets.UnmodifiableSortedSet.class);
        READ_ONLY.add(LongSortedSets.EmptySet.class);
        READ_ONLY.add(LongSortedSets.Singleton.class);
        READ_ONLY.add(LongBigLists.UnmodifiableBigList.class);
        READ_ONLY.add(LongBigLists.EmptyBigList.class);
        READ_ONLY.add(LongBigLists.Singleton.class);
        READ_ONLY.add(LongImmutableList.class);
        READ_ONLY.add(new LongImmutableList(new long[] { 0, 1 }).subList(0, 1).getClass());
        READ_ONLY.add(Float2BooleanSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Float2BooleanSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Float2BooleanSortedMaps.Singleton.class);
        READ_ONLY.add(Float2BooleanMaps.UnmodifiableMap.class);
        READ_ONLY.add(Float2BooleanMaps.EmptyMap.class);
        READ_ONLY.add(Float2BooleanMaps.Singleton.class);
        READ_ONLY.add(Float2ByteSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Float2ByteSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Float2ByteSortedMaps.Singleton.class);
        READ_ONLY.add(Float2ByteMaps.UnmodifiableMap.class);
        READ_ONLY.add(Float2ByteMaps.EmptyMap.class);
        READ_ONLY.add(Float2ByteMaps.Singleton.class);
        READ_ONLY.add(Float2ShortSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Float2ShortSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Float2ShortSortedMaps.Singleton.class);
        READ_ONLY.add(Float2ShortMaps.UnmodifiableMap.class);
        READ_ONLY.add(Float2ShortMaps.EmptyMap.class);
        READ_ONLY.add(Float2ShortMaps.Singleton.class);
        READ_ONLY.add(Float2CharSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Float2CharSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Float2CharSortedMaps.Singleton.class);
        READ_ONLY.add(Float2CharMaps.UnmodifiableMap.class);
        READ_ONLY.add(Float2CharMaps.EmptyMap.class);
        READ_ONLY.add(Float2CharMaps.Singleton.class);
        READ_ONLY.add(Float2IntSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Float2IntSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Float2IntSortedMaps.Singleton.class);
        READ_ONLY.add(Float2IntMaps.UnmodifiableMap.class);
        READ_ONLY.add(Float2IntMaps.EmptyMap.class);
        READ_ONLY.add(Float2IntMaps.Singleton.class);
        READ_ONLY.add(Float2LongSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Float2LongSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Float2LongSortedMaps.Singleton.class);
        READ_ONLY.add(Float2LongMaps.UnmodifiableMap.class);
        READ_ONLY.add(Float2LongMaps.EmptyMap.class);
        READ_ONLY.add(Float2LongMaps.Singleton.class);
        READ_ONLY.add(Float2FloatSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Float2FloatSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Float2FloatSortedMaps.Singleton.class);
        READ_ONLY.add(Float2FloatMaps.UnmodifiableMap.class);
        READ_ONLY.add(Float2FloatMaps.EmptyMap.class);
        READ_ONLY.add(Float2FloatMaps.Singleton.class);
        READ_ONLY.add(Float2DoubleSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Float2DoubleSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Float2DoubleSortedMaps.Singleton.class);
        READ_ONLY.add(Float2DoubleMaps.UnmodifiableMap.class);
        READ_ONLY.add(Float2DoubleMaps.EmptyMap.class);
        READ_ONLY.add(Float2DoubleMaps.Singleton.class);
        READ_ONLY.add(Float2ObjectSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Float2ObjectSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Float2ObjectSortedMaps.Singleton.class);
        READ_ONLY.add(Float2ObjectMaps.UnmodifiableMap.class);
        READ_ONLY.add(Float2ObjectMaps.EmptyMap.class);
        READ_ONLY.add(Float2ObjectMaps.Singleton.class);
        READ_ONLY.add(Float2ReferenceSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Float2ReferenceSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Float2ReferenceSortedMaps.Singleton.class);
        READ_ONLY.add(Float2ReferenceMaps.UnmodifiableMap.class);
        READ_ONLY.add(Float2ReferenceMaps.EmptyMap.class);
        READ_ONLY.add(Float2ReferenceMaps.Singleton.class);
        READ_ONLY.add(FloatCollections.unmodifiable(new FloatArrayList()).getClass());
        READ_ONLY.add(FloatLists.UnmodifiableList.class);
        READ_ONLY.add(FloatLists.EmptyList.class);
        READ_ONLY.add(FloatLists.Singleton.class);
        READ_ONLY.add(FloatLists.UnmodifiableRandomAccessList.class);
        READ_ONLY.add(FloatSets.UnmodifiableSet.class);
        READ_ONLY.add(FloatSets.EmptySet.class);
        READ_ONLY.add(FloatSets.Singleton.class);
        READ_ONLY.add(FloatSortedSets.UnmodifiableSortedSet.class);
        READ_ONLY.add(FloatSortedSets.EmptySet.class);
        READ_ONLY.add(FloatSortedSets.Singleton.class);
        READ_ONLY.add(FloatBigLists.UnmodifiableBigList.class);
        READ_ONLY.add(FloatBigLists.EmptyBigList.class);
        READ_ONLY.add(FloatBigLists.Singleton.class);
        READ_ONLY.add(FloatImmutableList.class);
        READ_ONLY.add(new FloatImmutableList(new float[] { 0, 1 }).subList(0, 1).getClass());
        READ_ONLY.add(Double2BooleanSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Double2BooleanSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Double2BooleanSortedMaps.Singleton.class);
        READ_ONLY.add(Double2BooleanMaps.UnmodifiableMap.class);
        READ_ONLY.add(Double2BooleanMaps.EmptyMap.class);
        READ_ONLY.add(Double2BooleanMaps.Singleton.class);
        READ_ONLY.add(Double2ByteSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Double2ByteSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Double2ByteSortedMaps.Singleton.class);
        READ_ONLY.add(Double2ByteMaps.UnmodifiableMap.class);
        READ_ONLY.add(Double2ByteMaps.EmptyMap.class);
        READ_ONLY.add(Double2ByteMaps.Singleton.class);
        READ_ONLY.add(Double2ShortSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Double2ShortSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Double2ShortSortedMaps.Singleton.class);
        READ_ONLY.add(Double2ShortMaps.UnmodifiableMap.class);
        READ_ONLY.add(Double2ShortMaps.EmptyMap.class);
        READ_ONLY.add(Double2ShortMaps.Singleton.class);
        READ_ONLY.add(Double2CharSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Double2CharSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Double2CharSortedMaps.Singleton.class);
        READ_ONLY.add(Double2CharMaps.UnmodifiableMap.class);
        READ_ONLY.add(Double2CharMaps.EmptyMap.class);
        READ_ONLY.add(Double2CharMaps.Singleton.class);
        READ_ONLY.add(Double2IntSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Double2IntSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Double2IntSortedMaps.Singleton.class);
        READ_ONLY.add(Double2IntMaps.UnmodifiableMap.class);
        READ_ONLY.add(Double2IntMaps.EmptyMap.class);
        READ_ONLY.add(Double2IntMaps.Singleton.class);
        READ_ONLY.add(Double2LongSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Double2LongSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Double2LongSortedMaps.Singleton.class);
        READ_ONLY.add(Double2LongMaps.UnmodifiableMap.class);
        READ_ONLY.add(Double2LongMaps.EmptyMap.class);
        READ_ONLY.add(Double2LongMaps.Singleton.class);
        READ_ONLY.add(Double2FloatSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Double2FloatSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Double2FloatSortedMaps.Singleton.class);
        READ_ONLY.add(Double2FloatMaps.UnmodifiableMap.class);
        READ_ONLY.add(Double2FloatMaps.EmptyMap.class);
        READ_ONLY.add(Double2FloatMaps.Singleton.class);
        READ_ONLY.add(Double2DoubleSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Double2DoubleSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Double2DoubleSortedMaps.Singleton.class);
        READ_ONLY.add(Double2DoubleMaps.UnmodifiableMap.class);
        READ_ONLY.add(Double2DoubleMaps.EmptyMap.class);
        READ_ONLY.add(Double2DoubleMaps.Singleton.class);
        READ_ONLY.add(Double2ObjectSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Double2ObjectSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Double2ObjectSortedMaps.Singleton.class);
        READ_ONLY.add(Double2ObjectMaps.UnmodifiableMap.class);
        READ_ONLY.add(Double2ObjectMaps.EmptyMap.class);
        READ_ONLY.add(Double2ObjectMaps.Singleton.class);
        READ_ONLY.add(Double2ReferenceSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Double2ReferenceSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Double2ReferenceSortedMaps.Singleton.class);
        READ_ONLY.add(Double2ReferenceMaps.UnmodifiableMap.class);
        READ_ONLY.add(Double2ReferenceMaps.EmptyMap.class);
        READ_ONLY.add(Double2ReferenceMaps.Singleton.class);
        READ_ONLY.add(DoubleCollections.unmodifiable(new DoubleArrayList()).getClass());
        READ_ONLY.add(DoubleLists.UnmodifiableList.class);
        READ_ONLY.add(DoubleLists.EmptyList.class);
        READ_ONLY.add(DoubleLists.Singleton.class);
        READ_ONLY.add(DoubleLists.UnmodifiableRandomAccessList.class);
        READ_ONLY.add(DoubleSets.UnmodifiableSet.class);
        READ_ONLY.add(DoubleSets.EmptySet.class);
        READ_ONLY.add(DoubleSets.Singleton.class);
        READ_ONLY.add(DoubleSortedSets.UnmodifiableSortedSet.class);
        READ_ONLY.add(DoubleSortedSets.EmptySet.class);
        READ_ONLY.add(DoubleSortedSets.Singleton.class);
        READ_ONLY.add(DoubleBigLists.UnmodifiableBigList.class);
        READ_ONLY.add(DoubleBigLists.EmptyBigList.class);
        READ_ONLY.add(DoubleBigLists.Singleton.class);
        READ_ONLY.add(DoubleImmutableList.class);
        READ_ONLY.add(new DoubleImmutableList(new double[] { 0, 1 }).subList(0, 1).getClass());
        READ_ONLY.add(Object2BooleanSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Object2BooleanSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Object2BooleanSortedMaps.Singleton.class);
        READ_ONLY.add(Object2BooleanMaps.UnmodifiableMap.class);
        READ_ONLY.add(Object2BooleanMaps.EmptyMap.class);
        READ_ONLY.add(Object2BooleanMaps.Singleton.class);
        READ_ONLY.add(Object2ByteSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Object2ByteSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Object2ByteSortedMaps.Singleton.class);
        READ_ONLY.add(Object2ByteMaps.UnmodifiableMap.class);
        READ_ONLY.add(Object2ByteMaps.EmptyMap.class);
        READ_ONLY.add(Object2ByteMaps.Singleton.class);
        READ_ONLY.add(Object2ShortSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Object2ShortSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Object2ShortSortedMaps.Singleton.class);
        READ_ONLY.add(Object2ShortMaps.UnmodifiableMap.class);
        READ_ONLY.add(Object2ShortMaps.EmptyMap.class);
        READ_ONLY.add(Object2ShortMaps.Singleton.class);
        READ_ONLY.add(Object2CharSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Object2CharSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Object2CharSortedMaps.Singleton.class);
        READ_ONLY.add(Object2CharMaps.UnmodifiableMap.class);
        READ_ONLY.add(Object2CharMaps.EmptyMap.class);
        READ_ONLY.add(Object2CharMaps.Singleton.class);
        READ_ONLY.add(Object2IntSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Object2IntSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Object2IntSortedMaps.Singleton.class);
        READ_ONLY.add(Object2IntMaps.UnmodifiableMap.class);
        READ_ONLY.add(Object2IntMaps.EmptyMap.class);
        READ_ONLY.add(Object2IntMaps.Singleton.class);
        READ_ONLY.add(Object2LongSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Object2LongSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Object2LongSortedMaps.Singleton.class);
        READ_ONLY.add(Object2LongMaps.UnmodifiableMap.class);
        READ_ONLY.add(Object2LongMaps.EmptyMap.class);
        READ_ONLY.add(Object2LongMaps.Singleton.class);
        READ_ONLY.add(Object2FloatSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Object2FloatSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Object2FloatSortedMaps.Singleton.class);
        READ_ONLY.add(Object2FloatMaps.UnmodifiableMap.class);
        READ_ONLY.add(Object2FloatMaps.EmptyMap.class);
        READ_ONLY.add(Object2FloatMaps.Singleton.class);
        READ_ONLY.add(Object2DoubleSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Object2DoubleSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Object2DoubleSortedMaps.Singleton.class);
        READ_ONLY.add(Object2DoubleMaps.UnmodifiableMap.class);
        READ_ONLY.add(Object2DoubleMaps.EmptyMap.class);
        READ_ONLY.add(Object2DoubleMaps.Singleton.class);
        READ_ONLY.add(Object2ObjectSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Object2ObjectSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Object2ObjectSortedMaps.Singleton.class);
        READ_ONLY.add(Object2ObjectMaps.UnmodifiableMap.class);
        READ_ONLY.add(Object2ObjectMaps.EmptyMap.class);
        READ_ONLY.add(Object2ObjectMaps.Singleton.class);
        READ_ONLY.add(Object2ReferenceSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Object2ReferenceSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Object2ReferenceSortedMaps.Singleton.class);
        READ_ONLY.add(Object2ReferenceMaps.UnmodifiableMap.class);
        READ_ONLY.add(Object2ReferenceMaps.EmptyMap.class);
        READ_ONLY.add(Object2ReferenceMaps.Singleton.class);
        READ_ONLY.add(ObjectCollections.unmodifiable(new ObjectArrayList()).getClass());
        READ_ONLY.add(ObjectLists.UnmodifiableList.class);
        READ_ONLY.add(ObjectLists.EmptyList.class);
        READ_ONLY.add(ObjectLists.Singleton.class);
        READ_ONLY.add(ObjectLists.UnmodifiableRandomAccessList.class);
        READ_ONLY.add(ObjectSets.UnmodifiableSet.class);
        READ_ONLY.add(ObjectSets.EmptySet.class);
        READ_ONLY.add(ObjectSets.Singleton.class);
        READ_ONLY.add(ObjectSortedSets.UnmodifiableSortedSet.class);
        READ_ONLY.add(ObjectSortedSets.EmptySet.class);
        READ_ONLY.add(ObjectSortedSets.Singleton.class);
        READ_ONLY.add(ObjectBigLists.UnmodifiableBigList.class);
        READ_ONLY.add(ObjectBigLists.EmptyBigList.class);
        READ_ONLY.add(ObjectBigLists.Singleton.class);
        READ_ONLY.add(ObjectImmutableList.class);
        READ_ONLY.add(new ObjectImmutableList(new Object[] { null, null }).subList(0, 1).getClass());
        READ_ONLY.add(Reference2BooleanSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Reference2BooleanSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Reference2BooleanSortedMaps.Singleton.class);
        READ_ONLY.add(Reference2BooleanMaps.UnmodifiableMap.class);
        READ_ONLY.add(Reference2BooleanMaps.EmptyMap.class);
        READ_ONLY.add(Reference2BooleanMaps.Singleton.class);
        READ_ONLY.add(Reference2ByteSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Reference2ByteSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Reference2ByteSortedMaps.Singleton.class);
        READ_ONLY.add(Reference2ByteMaps.UnmodifiableMap.class);
        READ_ONLY.add(Reference2ByteMaps.EmptyMap.class);
        READ_ONLY.add(Reference2ByteMaps.Singleton.class);
        READ_ONLY.add(Reference2ShortSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Reference2ShortSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Reference2ShortSortedMaps.Singleton.class);
        READ_ONLY.add(Reference2ShortMaps.UnmodifiableMap.class);
        READ_ONLY.add(Reference2ShortMaps.EmptyMap.class);
        READ_ONLY.add(Reference2ShortMaps.Singleton.class);
        READ_ONLY.add(Reference2CharSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Reference2CharSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Reference2CharSortedMaps.Singleton.class);
        READ_ONLY.add(Reference2CharMaps.UnmodifiableMap.class);
        READ_ONLY.add(Reference2CharMaps.EmptyMap.class);
        READ_ONLY.add(Reference2CharMaps.Singleton.class);
        READ_ONLY.add(Reference2IntSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Reference2IntSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Reference2IntSortedMaps.Singleton.class);
        READ_ONLY.add(Reference2IntMaps.UnmodifiableMap.class);
        READ_ONLY.add(Reference2IntMaps.EmptyMap.class);
        READ_ONLY.add(Reference2IntMaps.Singleton.class);
        READ_ONLY.add(Reference2LongSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Reference2LongSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Reference2LongSortedMaps.Singleton.class);
        READ_ONLY.add(Reference2LongMaps.UnmodifiableMap.class);
        READ_ONLY.add(Reference2LongMaps.EmptyMap.class);
        READ_ONLY.add(Reference2LongMaps.Singleton.class);
        READ_ONLY.add(Reference2FloatSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Reference2FloatSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Reference2FloatSortedMaps.Singleton.class);
        READ_ONLY.add(Reference2FloatMaps.UnmodifiableMap.class);
        READ_ONLY.add(Reference2FloatMaps.EmptyMap.class);
        READ_ONLY.add(Reference2FloatMaps.Singleton.class);
        READ_ONLY.add(Reference2DoubleSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Reference2DoubleSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Reference2DoubleSortedMaps.Singleton.class);
        READ_ONLY.add(Reference2DoubleMaps.UnmodifiableMap.class);
        READ_ONLY.add(Reference2DoubleMaps.EmptyMap.class);
        READ_ONLY.add(Reference2DoubleMaps.Singleton.class);
        READ_ONLY.add(Reference2ObjectSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Reference2ObjectSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Reference2ObjectSortedMaps.Singleton.class);
        READ_ONLY.add(Reference2ObjectMaps.UnmodifiableMap.class);
        READ_ONLY.add(Reference2ObjectMaps.EmptyMap.class);
        READ_ONLY.add(Reference2ObjectMaps.Singleton.class);
        READ_ONLY.add(Reference2ReferenceSortedMaps.UnmodifiableSortedMap.class);
        READ_ONLY.add(Reference2ReferenceSortedMaps.EmptySortedMap.class);
        READ_ONLY.add(Reference2ReferenceSortedMaps.Singleton.class);
        READ_ONLY.add(Reference2ReferenceMaps.UnmodifiableMap.class);
        READ_ONLY.add(Reference2ReferenceMaps.EmptyMap.class);
        READ_ONLY.add(Reference2ReferenceMaps.Singleton.class);
        READ_ONLY.add(ReferenceCollections.unmodifiable(new ReferenceArrayList()).getClass());
        READ_ONLY.add(ReferenceLists.UnmodifiableList.class);
        READ_ONLY.add(ReferenceLists.EmptyList.class);
        READ_ONLY.add(ReferenceLists.Singleton.class);
        READ_ONLY.add(ReferenceLists.UnmodifiableRandomAccessList.class);
        READ_ONLY.add(ReferenceSets.UnmodifiableSet.class);
        READ_ONLY.add(ReferenceSets.EmptySet.class);
        READ_ONLY.add(ReferenceSets.Singleton.class);
        READ_ONLY.add(ReferenceSortedSets.UnmodifiableSortedSet.class);
        READ_ONLY.add(ReferenceSortedSets.EmptySet.class);
        READ_ONLY.add(ReferenceSortedSets.Singleton.class);
        READ_ONLY.add(ReferenceBigLists.UnmodifiableBigList.class);
        READ_ONLY.add(ReferenceBigLists.EmptyBigList.class);
        READ_ONLY.add(ReferenceBigLists.Singleton.class);
        READ_ONLY.add(ReferenceImmutableList.class);
        READ_ONLY.add(new ReferenceImmutableList(new Object[] { null, null }).subList(0, 1).getClass());
    }

    private FastutilCollections() {}

    public static Construction construction(Type type, Object current) {
        var factory = FACTORIES.get(current == null ? Types.raw(type) : current.getClass());
        if (factory == null) factory = FACTORIES.get(Types.raw(type));
        return factory == null ? null : factory.apply(type, current);
    }

    public static boolean isReadOnly(Object value) {
        return READ_ONLY.contains(value.getClass());
    }

    private static void register(Class<?> type, BiFunction<Type, Object, Construction> factory) {
        FACTORIES.put(type, factory);
    }

    private static Object updateMapped(BigList current, BigList decoded) {
        if (current.size64() != decoded.size64())
            throw new IllegalArgumentException("A mapped big list cannot resize its backing file");
        for (long index = 0; index < decoded.size64(); index++) current.set(index, decoded.get(index));
        return current;
    }

    private static void registerBoolean() {
        register(BooleanCollection.class, (type, current) -> new Construction(() -> new BooleanArrayList()));
        register(BooleanList.class, (type, current) -> new Construction(() -> new BooleanArrayList()));
        register(BooleanSet.class, (type, current) -> new Construction(() -> new BooleanOpenHashSet()));
        register(BooleanBigList.class, (type, current) -> new Construction(() -> new BooleanBigArrayBigList()));
        register(BooleanArrayList.class, (type, current) -> new Construction(
                () -> current == null ? new BooleanArrayList() : ((BooleanArrayList) current).clone()));
        register(BooleanArraySet.class, (type, current) -> new Construction(
                () -> current == null ? new BooleanArraySet() : ((BooleanArraySet) current).clone()));
        register(BooleanOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new BooleanOpenHashSet() : ((BooleanOpenHashSet) current).clone()));
        register(BooleanBigArrayBigList.class, (type, current) -> new Construction(
                () -> current == null ? new BooleanBigArrayBigList() : ((BooleanBigArrayBigList) current).clone()));
        register(BooleanCollections.unmodifiable(new BooleanArrayList()).getClass(),
                (type, current) -> new Construction(() -> new BooleanArrayList(),
                        decoded -> BooleanCollections.unmodifiable((BooleanCollection) decoded)));
        register(BooleanCollections.unmodifiable(new BooleanArrayList()).getClass(),
                (type, current) -> new Construction(() -> new BooleanArrayList(),
                        decoded -> BooleanCollections.unmodifiable((BooleanCollection) decoded)));
        register(BooleanCollections.synchronize(new BooleanArrayList()).getClass(),
                (type, current) -> new Construction(() -> new BooleanArrayList(),
                        decoded -> BooleanCollections.synchronize((BooleanCollection) decoded)));
        register(BooleanLists.UnmodifiableList.class, (type, current) -> new Construction(() -> new BooleanArrayList(),
                decoded -> BooleanLists.unmodifiable((BooleanList) decoded)));
        register(BooleanLists.EmptyList.class, (type, current) -> new Construction(() -> new BooleanArrayList(),
                decoded -> BooleanLists.unmodifiable((BooleanList) decoded)));
        register(BooleanLists.Singleton.class, (type, current) -> new Construction(() -> new BooleanArrayList(),
                decoded -> BooleanLists.unmodifiable((BooleanList) decoded)));
        register(BooleanLists.SynchronizedList.class, (type, current) -> new Construction(() -> new BooleanArrayList(),
                decoded -> BooleanLists.synchronize((BooleanList) decoded)));
        register(BooleanLists.UnmodifiableRandomAccessList.class,
                (type, current) -> new Construction(() -> new BooleanArrayList(),
                        decoded -> BooleanLists.unmodifiable((BooleanList) decoded)));
        register(BooleanLists.SynchronizedRandomAccessList.class,
                (type, current) -> new Construction(() -> new BooleanArrayList(),
                        decoded -> BooleanLists.synchronize((BooleanList) decoded)));
        register(BooleanSets.UnmodifiableSet.class, (type, current) -> new Construction(() -> new BooleanOpenHashSet(),
                decoded -> BooleanSets.unmodifiable((BooleanSet) decoded)));
        register(BooleanSets.EmptySet.class, (type, current) -> new Construction(() -> new BooleanOpenHashSet(),
                decoded -> BooleanSets.unmodifiable((BooleanSet) decoded)));
        register(BooleanSets.Singleton.class, (type, current) -> new Construction(() -> new BooleanOpenHashSet(),
                decoded -> BooleanSets.unmodifiable((BooleanSet) decoded)));
        register(BooleanSets.SynchronizedSet.class, (type, current) -> new Construction(() -> new BooleanOpenHashSet(),
                decoded -> BooleanSets.synchronize((BooleanSet) decoded)));
        register(BooleanBigLists.UnmodifiableBigList.class,
                (type, current) -> new Construction(() -> new BooleanBigArrayBigList(),
                        decoded -> BooleanBigLists.unmodifiable((BooleanBigList) decoded)));
        register(BooleanBigLists.EmptyBigList.class,
                (type, current) -> new Construction(() -> new BooleanBigArrayBigList(),
                        decoded -> BooleanBigLists.unmodifiable((BooleanBigList) decoded)));
        register(BooleanBigLists.Singleton.class,
                (type, current) -> new Construction(() -> new BooleanBigArrayBigList(),
                        decoded -> BooleanBigLists.unmodifiable((BooleanBigList) decoded)));
        register(BooleanBigLists.SynchronizedBigList.class,
                (type, current) -> new Construction(() -> new BooleanBigArrayBigList(),
                        decoded -> BooleanBigLists.synchronize((BooleanBigList) decoded)));
        register(BooleanImmutableList.class, (type, current) -> new Construction(() -> new BooleanArrayList(),
                decoded -> new BooleanImmutableList((Collection) decoded)));
        register(new BooleanImmutableList(new boolean[] { false, true }).subList(0, 1).getClass(),
                (type, current) -> new Construction(() -> new BooleanArrayList(),
                        decoded -> new BooleanImmutableList((Collection) decoded)));
    }

    private static void registerByte() {
        register(Byte2BooleanMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2BooleanMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2BooleanOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2BooleanOpenHashMap() : ((Byte2BooleanOpenHashMap) current).clone()));
        register(Byte2BooleanArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2BooleanArrayMap() : ((Byte2BooleanArrayMap) current).clone()));
        register(Byte2BooleanLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Byte2BooleanLinkedOpenHashMap() : ((Byte2BooleanLinkedOpenHashMap) current).clone()));
        register(Byte2BooleanAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2BooleanAVLTreeMap() : ((Byte2BooleanAVLTreeMap) current).clone()));
        register(Byte2BooleanRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2BooleanRBTreeMap() : ((Byte2BooleanRBTreeMap) current).clone()));
        register(Byte2BooleanOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Byte2BooleanOpenCustomHashMap) current).clone()));
        register(Byte2BooleanSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2BooleanAVLTreeMap();
            var result = new Byte2BooleanAVLTreeMap(((Byte2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2BooleanSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2BooleanAVLTreeMap();
            var result = new Byte2BooleanAVLTreeMap(((Byte2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2BooleanSortedMaps.unmodifiable((Byte2BooleanSortedMap) decoded)));
        register(Byte2BooleanSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2BooleanAVLTreeMap();
            var result = new Byte2BooleanAVLTreeMap(((Byte2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2BooleanSortedMaps.unmodifiable((Byte2BooleanSortedMap) decoded)));
        register(Byte2BooleanSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2BooleanAVLTreeMap();
            var result = new Byte2BooleanAVLTreeMap(((Byte2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2BooleanSortedMaps.unmodifiable((Byte2BooleanSortedMap) decoded)));
        register(Byte2BooleanSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2BooleanAVLTreeMap();
            var result = new Byte2BooleanAVLTreeMap(((Byte2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2BooleanSortedMaps.synchronize((Byte2BooleanSortedMap) decoded)));
        register(Byte2BooleanMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2BooleanMaps.unmodifiable((Byte2BooleanMap) decoded)));
        register(Byte2BooleanMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2BooleanMaps.unmodifiable((Byte2BooleanMap) decoded)));
        register(Byte2BooleanMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Byte2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2BooleanMaps.unmodifiable((Byte2BooleanMap) decoded)));
        register(Byte2BooleanMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2BooleanMaps.synchronize((Byte2BooleanMap) decoded)));
        register(Byte2ByteMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ByteMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2ByteOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ByteOpenHashMap() : ((Byte2ByteOpenHashMap) current).clone()));
        register(Byte2ByteArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ByteArrayMap() : ((Byte2ByteArrayMap) current).clone()));
        register(Byte2ByteLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Byte2ByteLinkedOpenHashMap() : ((Byte2ByteLinkedOpenHashMap) current).clone()));
        register(Byte2ByteAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ByteAVLTreeMap() : ((Byte2ByteAVLTreeMap) current).clone()));
        register(Byte2ByteRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ByteRBTreeMap() : ((Byte2ByteRBTreeMap) current).clone()));
        register(Byte2ByteOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Byte2ByteOpenCustomHashMap) current).clone()));
        register(Byte2ByteSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ByteAVLTreeMap();
            var result = new Byte2ByteAVLTreeMap(((Byte2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ByteSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2ByteSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ByteAVLTreeMap();
            var result = new Byte2ByteAVLTreeMap(((Byte2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ByteSortedMaps.unmodifiable((Byte2ByteSortedMap) decoded)));
        register(Byte2ByteSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ByteAVLTreeMap();
            var result = new Byte2ByteAVLTreeMap(((Byte2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ByteSortedMaps.unmodifiable((Byte2ByteSortedMap) decoded)));
        register(Byte2ByteSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ByteAVLTreeMap();
            var result = new Byte2ByteAVLTreeMap(((Byte2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ByteSortedMaps.unmodifiable((Byte2ByteSortedMap) decoded)));
        register(Byte2ByteSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ByteAVLTreeMap();
            var result = new Byte2ByteAVLTreeMap(((Byte2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ByteSortedMaps.synchronize((Byte2ByteSortedMap) decoded)));
        register(Byte2ByteMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ByteMaps.unmodifiable((Byte2ByteMap) decoded)));
        register(Byte2ByteMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ByteMaps.unmodifiable((Byte2ByteMap) decoded)));
        register(Byte2ByteMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ByteMaps.unmodifiable((Byte2ByteMap) decoded)));
        register(Byte2ByteMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ByteMaps.synchronize((Byte2ByteMap) decoded)));
        register(Byte2ShortMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ShortMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2ShortOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ShortOpenHashMap() : ((Byte2ShortOpenHashMap) current).clone()));
        register(Byte2ShortArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ShortArrayMap() : ((Byte2ShortArrayMap) current).clone()));
        register(Byte2ShortLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Byte2ShortLinkedOpenHashMap() : ((Byte2ShortLinkedOpenHashMap) current).clone()));
        register(Byte2ShortAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ShortAVLTreeMap() : ((Byte2ShortAVLTreeMap) current).clone()));
        register(Byte2ShortRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ShortRBTreeMap() : ((Byte2ShortRBTreeMap) current).clone()));
        register(Byte2ShortOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Byte2ShortOpenCustomHashMap) current).clone()));
        register(Byte2ShortSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ShortAVLTreeMap();
            var result = new Byte2ShortAVLTreeMap(((Byte2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ShortSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2ShortSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ShortAVLTreeMap();
            var result = new Byte2ShortAVLTreeMap(((Byte2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ShortSortedMaps.unmodifiable((Byte2ShortSortedMap) decoded)));
        register(Byte2ShortSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ShortAVLTreeMap();
            var result = new Byte2ShortAVLTreeMap(((Byte2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ShortSortedMaps.unmodifiable((Byte2ShortSortedMap) decoded)));
        register(Byte2ShortSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ShortAVLTreeMap();
            var result = new Byte2ShortAVLTreeMap(((Byte2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ShortSortedMaps.unmodifiable((Byte2ShortSortedMap) decoded)));
        register(Byte2ShortSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ShortAVLTreeMap();
            var result = new Byte2ShortAVLTreeMap(((Byte2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ShortSortedMaps.synchronize((Byte2ShortSortedMap) decoded)));
        register(Byte2ShortMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ShortMaps.unmodifiable((Byte2ShortMap) decoded)));
        register(Byte2ShortMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ShortMaps.unmodifiable((Byte2ShortMap) decoded)));
        register(Byte2ShortMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ShortMaps.unmodifiable((Byte2ShortMap) decoded)));
        register(Byte2ShortMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ShortMaps.synchronize((Byte2ShortMap) decoded)));
        register(Byte2CharMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2CharMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2CharOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2CharOpenHashMap() : ((Byte2CharOpenHashMap) current).clone()));
        register(Byte2CharArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2CharArrayMap() : ((Byte2CharArrayMap) current).clone()));
        register(Byte2CharLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Byte2CharLinkedOpenHashMap() : ((Byte2CharLinkedOpenHashMap) current).clone()));
        register(Byte2CharAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2CharAVLTreeMap() : ((Byte2CharAVLTreeMap) current).clone()));
        register(Byte2CharRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2CharRBTreeMap() : ((Byte2CharRBTreeMap) current).clone()));
        register(Byte2CharOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Byte2CharOpenCustomHashMap) current).clone()));
        register(Byte2CharSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2CharAVLTreeMap();
            var result = new Byte2CharAVLTreeMap(((Byte2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2CharSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2CharSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2CharAVLTreeMap();
            var result = new Byte2CharAVLTreeMap(((Byte2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2CharSortedMaps.unmodifiable((Byte2CharSortedMap) decoded)));
        register(Byte2CharSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2CharAVLTreeMap();
            var result = new Byte2CharAVLTreeMap(((Byte2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2CharSortedMaps.unmodifiable((Byte2CharSortedMap) decoded)));
        register(Byte2CharSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2CharAVLTreeMap();
            var result = new Byte2CharAVLTreeMap(((Byte2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2CharSortedMaps.unmodifiable((Byte2CharSortedMap) decoded)));
        register(Byte2CharSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2CharAVLTreeMap();
            var result = new Byte2CharAVLTreeMap(((Byte2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2CharSortedMaps.synchronize((Byte2CharSortedMap) decoded)));
        register(Byte2CharMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2CharMaps.unmodifiable((Byte2CharMap) decoded)));
        register(Byte2CharMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2CharMaps.unmodifiable((Byte2CharMap) decoded)));
        register(Byte2CharMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Byte2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2CharMaps.unmodifiable((Byte2CharMap) decoded)));
        register(Byte2CharMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2CharMaps.synchronize((Byte2CharMap) decoded)));
        register(Byte2IntMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2IntMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2IntOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2IntOpenHashMap() : ((Byte2IntOpenHashMap) current).clone()));
        register(Byte2IntArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2IntArrayMap() : ((Byte2IntArrayMap) current).clone()));
        register(Byte2IntLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Byte2IntLinkedOpenHashMap() : ((Byte2IntLinkedOpenHashMap) current).clone()));
        register(Byte2IntAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2IntAVLTreeMap() : ((Byte2IntAVLTreeMap) current).clone()));
        register(Byte2IntRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2IntRBTreeMap() : ((Byte2IntRBTreeMap) current).clone()));
        register(Byte2IntOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Byte2IntOpenCustomHashMap) current).clone()));
        register(Byte2IntSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2IntAVLTreeMap();
            var result = new Byte2IntAVLTreeMap(((Byte2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2IntSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2IntSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2IntAVLTreeMap();
            var result = new Byte2IntAVLTreeMap(((Byte2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2IntSortedMaps.unmodifiable((Byte2IntSortedMap) decoded)));
        register(Byte2IntSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2IntAVLTreeMap();
            var result = new Byte2IntAVLTreeMap(((Byte2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2IntSortedMaps.unmodifiable((Byte2IntSortedMap) decoded)));
        register(Byte2IntSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2IntAVLTreeMap();
            var result = new Byte2IntAVLTreeMap(((Byte2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2IntSortedMaps.unmodifiable((Byte2IntSortedMap) decoded)));
        register(Byte2IntSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2IntAVLTreeMap();
            var result = new Byte2IntAVLTreeMap(((Byte2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2IntSortedMaps.synchronize((Byte2IntSortedMap) decoded)));
        register(Byte2IntMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2IntMaps.unmodifiable((Byte2IntMap) decoded)));
        register(Byte2IntMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2IntMaps.unmodifiable((Byte2IntMap) decoded)));
        register(Byte2IntMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Byte2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2IntMaps.unmodifiable((Byte2IntMap) decoded)));
        register(Byte2IntMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2IntMaps.synchronize((Byte2IntMap) decoded)));
        register(Byte2LongMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2LongMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2LongOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2LongOpenHashMap() : ((Byte2LongOpenHashMap) current).clone()));
        register(Byte2LongArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2LongArrayMap() : ((Byte2LongArrayMap) current).clone()));
        register(Byte2LongLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Byte2LongLinkedOpenHashMap() : ((Byte2LongLinkedOpenHashMap) current).clone()));
        register(Byte2LongAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2LongAVLTreeMap() : ((Byte2LongAVLTreeMap) current).clone()));
        register(Byte2LongRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2LongRBTreeMap() : ((Byte2LongRBTreeMap) current).clone()));
        register(Byte2LongOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Byte2LongOpenCustomHashMap) current).clone()));
        register(Byte2LongSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2LongAVLTreeMap();
            var result = new Byte2LongAVLTreeMap(((Byte2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2LongSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2LongSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2LongAVLTreeMap();
            var result = new Byte2LongAVLTreeMap(((Byte2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2LongSortedMaps.unmodifiable((Byte2LongSortedMap) decoded)));
        register(Byte2LongSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2LongAVLTreeMap();
            var result = new Byte2LongAVLTreeMap(((Byte2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2LongSortedMaps.unmodifiable((Byte2LongSortedMap) decoded)));
        register(Byte2LongSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2LongAVLTreeMap();
            var result = new Byte2LongAVLTreeMap(((Byte2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2LongSortedMaps.unmodifiable((Byte2LongSortedMap) decoded)));
        register(Byte2LongSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2LongAVLTreeMap();
            var result = new Byte2LongAVLTreeMap(((Byte2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2LongSortedMaps.synchronize((Byte2LongSortedMap) decoded)));
        register(Byte2LongMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2LongMaps.unmodifiable((Byte2LongMap) decoded)));
        register(Byte2LongMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2LongMaps.unmodifiable((Byte2LongMap) decoded)));
        register(Byte2LongMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Byte2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2LongMaps.unmodifiable((Byte2LongMap) decoded)));
        register(Byte2LongMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2LongMaps.synchronize((Byte2LongMap) decoded)));
        register(Byte2FloatMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2FloatMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2FloatOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2FloatOpenHashMap() : ((Byte2FloatOpenHashMap) current).clone()));
        register(Byte2FloatArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2FloatArrayMap() : ((Byte2FloatArrayMap) current).clone()));
        register(Byte2FloatLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Byte2FloatLinkedOpenHashMap() : ((Byte2FloatLinkedOpenHashMap) current).clone()));
        register(Byte2FloatAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2FloatAVLTreeMap() : ((Byte2FloatAVLTreeMap) current).clone()));
        register(Byte2FloatRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2FloatRBTreeMap() : ((Byte2FloatRBTreeMap) current).clone()));
        register(Byte2FloatOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Byte2FloatOpenCustomHashMap) current).clone()));
        register(Byte2FloatSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2FloatAVLTreeMap();
            var result = new Byte2FloatAVLTreeMap(((Byte2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2FloatSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2FloatSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2FloatAVLTreeMap();
            var result = new Byte2FloatAVLTreeMap(((Byte2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2FloatSortedMaps.unmodifiable((Byte2FloatSortedMap) decoded)));
        register(Byte2FloatSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2FloatAVLTreeMap();
            var result = new Byte2FloatAVLTreeMap(((Byte2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2FloatSortedMaps.unmodifiable((Byte2FloatSortedMap) decoded)));
        register(Byte2FloatSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2FloatAVLTreeMap();
            var result = new Byte2FloatAVLTreeMap(((Byte2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2FloatSortedMaps.unmodifiable((Byte2FloatSortedMap) decoded)));
        register(Byte2FloatSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2FloatAVLTreeMap();
            var result = new Byte2FloatAVLTreeMap(((Byte2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2FloatSortedMaps.synchronize((Byte2FloatSortedMap) decoded)));
        register(Byte2FloatMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2FloatMaps.unmodifiable((Byte2FloatMap) decoded)));
        register(Byte2FloatMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2FloatMaps.unmodifiable((Byte2FloatMap) decoded)));
        register(Byte2FloatMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Byte2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2FloatMaps.unmodifiable((Byte2FloatMap) decoded)));
        register(Byte2FloatMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2FloatMaps.synchronize((Byte2FloatMap) decoded)));
        register(Byte2DoubleMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2DoubleMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2DoubleOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2DoubleOpenHashMap() : ((Byte2DoubleOpenHashMap) current).clone()));
        register(Byte2DoubleArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2DoubleArrayMap() : ((Byte2DoubleArrayMap) current).clone()));
        register(Byte2DoubleLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Byte2DoubleLinkedOpenHashMap() : ((Byte2DoubleLinkedOpenHashMap) current).clone()));
        register(Byte2DoubleAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2DoubleAVLTreeMap() : ((Byte2DoubleAVLTreeMap) current).clone()));
        register(Byte2DoubleRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2DoubleRBTreeMap() : ((Byte2DoubleRBTreeMap) current).clone()));
        register(Byte2DoubleOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Byte2DoubleOpenCustomHashMap) current).clone()));
        register(Byte2DoubleSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2DoubleAVLTreeMap();
            var result = new Byte2DoubleAVLTreeMap(((Byte2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2DoubleSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2DoubleAVLTreeMap();
            var result = new Byte2DoubleAVLTreeMap(((Byte2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2DoubleSortedMaps.unmodifiable((Byte2DoubleSortedMap) decoded)));
        register(Byte2DoubleSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2DoubleAVLTreeMap();
            var result = new Byte2DoubleAVLTreeMap(((Byte2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2DoubleSortedMaps.unmodifiable((Byte2DoubleSortedMap) decoded)));
        register(Byte2DoubleSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2DoubleAVLTreeMap();
            var result = new Byte2DoubleAVLTreeMap(((Byte2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2DoubleSortedMaps.unmodifiable((Byte2DoubleSortedMap) decoded)));
        register(Byte2DoubleSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2DoubleAVLTreeMap();
            var result = new Byte2DoubleAVLTreeMap(((Byte2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2DoubleSortedMaps.synchronize((Byte2DoubleSortedMap) decoded)));
        register(Byte2DoubleMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2DoubleMaps.unmodifiable((Byte2DoubleMap) decoded)));
        register(Byte2DoubleMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2DoubleMaps.unmodifiable((Byte2DoubleMap) decoded)));
        register(Byte2DoubleMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Byte2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2DoubleMaps.unmodifiable((Byte2DoubleMap) decoded)));
        register(Byte2DoubleMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2DoubleMaps.synchronize((Byte2DoubleMap) decoded)));
        register(Byte2ObjectMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ObjectMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2ObjectOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ObjectOpenHashMap() : ((Byte2ObjectOpenHashMap) current).clone()));
        register(Byte2ObjectArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ObjectArrayMap() : ((Byte2ObjectArrayMap) current).clone()));
        register(Byte2ObjectLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Byte2ObjectLinkedOpenHashMap() : ((Byte2ObjectLinkedOpenHashMap) current).clone()));
        register(Byte2ObjectAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ObjectAVLTreeMap() : ((Byte2ObjectAVLTreeMap) current).clone()));
        register(Byte2ObjectRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ObjectRBTreeMap() : ((Byte2ObjectRBTreeMap) current).clone()));
        register(Byte2ObjectOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Byte2ObjectOpenCustomHashMap) current).clone()));
        register(Byte2ObjectSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ObjectAVLTreeMap();
            var result = new Byte2ObjectAVLTreeMap(((Byte2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2ObjectSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ObjectAVLTreeMap();
            var result = new Byte2ObjectAVLTreeMap(((Byte2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ObjectSortedMaps.unmodifiable((Byte2ObjectSortedMap) decoded)));
        register(Byte2ObjectSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ObjectAVLTreeMap();
            var result = new Byte2ObjectAVLTreeMap(((Byte2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ObjectSortedMaps.unmodifiable((Byte2ObjectSortedMap) decoded)));
        register(Byte2ObjectSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ObjectAVLTreeMap();
            var result = new Byte2ObjectAVLTreeMap(((Byte2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ObjectSortedMaps.unmodifiable((Byte2ObjectSortedMap) decoded)));
        register(Byte2ObjectSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ObjectAVLTreeMap();
            var result = new Byte2ObjectAVLTreeMap(((Byte2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ObjectSortedMaps.synchronize((Byte2ObjectSortedMap) decoded)));
        register(Byte2ObjectMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ObjectMaps.unmodifiable((Byte2ObjectMap) decoded)));
        register(Byte2ObjectMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ObjectMaps.unmodifiable((Byte2ObjectMap) decoded)));
        register(Byte2ObjectMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ObjectMaps.unmodifiable((Byte2ObjectMap) decoded)));
        register(Byte2ObjectMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ObjectMaps.synchronize((Byte2ObjectMap) decoded)));
        register(Byte2ReferenceMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ReferenceMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2ReferenceOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Byte2ReferenceOpenHashMap() : ((Byte2ReferenceOpenHashMap) current).clone()));
        register(Byte2ReferenceArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ReferenceArrayMap() : ((Byte2ReferenceArrayMap) current).clone()));
        register(Byte2ReferenceLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Byte2ReferenceLinkedOpenHashMap() : ((Byte2ReferenceLinkedOpenHashMap) current).clone()));
        register(Byte2ReferenceAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ReferenceAVLTreeMap() : ((Byte2ReferenceAVLTreeMap) current).clone()));
        register(Byte2ReferenceRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Byte2ReferenceRBTreeMap() : ((Byte2ReferenceRBTreeMap) current).clone()));
        register(Byte2ReferenceOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Byte2ReferenceOpenCustomHashMap) current).clone()));
        register(Byte2ReferenceSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ReferenceAVLTreeMap();
            var result = new Byte2ReferenceAVLTreeMap(((Byte2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Byte2ReferenceSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ReferenceAVLTreeMap();
            var result = new Byte2ReferenceAVLTreeMap(((Byte2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ReferenceSortedMaps.unmodifiable((Byte2ReferenceSortedMap) decoded)));
        register(Byte2ReferenceSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ReferenceAVLTreeMap();
            var result = new Byte2ReferenceAVLTreeMap(((Byte2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ReferenceSortedMaps.unmodifiable((Byte2ReferenceSortedMap) decoded)));
        register(Byte2ReferenceSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ReferenceAVLTreeMap();
            var result = new Byte2ReferenceAVLTreeMap(((Byte2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ReferenceSortedMaps.unmodifiable((Byte2ReferenceSortedMap) decoded)));
        register(Byte2ReferenceSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Byte2ReferenceAVLTreeMap();
            var result = new Byte2ReferenceAVLTreeMap(((Byte2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Byte2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ReferenceSortedMaps.synchronize((Byte2ReferenceSortedMap) decoded)));
        register(Byte2ReferenceMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ReferenceMaps.unmodifiable((Byte2ReferenceMap) decoded)));
        register(Byte2ReferenceMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ReferenceMaps.unmodifiable((Byte2ReferenceMap) decoded)));
        register(Byte2ReferenceMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ReferenceMaps.unmodifiable((Byte2ReferenceMap) decoded)));
        register(Byte2ReferenceMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Byte2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Byte2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Byte2ReferenceMaps.synchronize((Byte2ReferenceMap) decoded)));
        register(ByteCollection.class, (type, current) -> new Construction(() -> new ByteArrayList()));
        register(ByteList.class, (type, current) -> new Construction(() -> new ByteArrayList()));
        register(ByteSet.class, (type, current) -> new Construction(() -> new ByteOpenHashSet()));
        register(ByteSortedSet.class, (type, current) -> new Construction(() -> current == null ? new ByteRBTreeSet() :
                new ByteRBTreeSet(((ByteSortedSet) current).comparator())));
        register(ByteBigList.class, (type, current) -> new Construction(() -> new ByteBigArrayBigList()));
        register(ByteArrayList.class, (type, current) -> new Construction(
                () -> current == null ? new ByteArrayList() : ((ByteArrayList) current).clone()));
        register(ByteArraySet.class, (type, current) -> new Construction(
                () -> current == null ? new ByteArraySet() : ((ByteArraySet) current).clone()));
        register(ByteOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new ByteOpenHashSet() : ((ByteOpenHashSet) current).clone()));
        register(ByteLinkedOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new ByteLinkedOpenHashSet() : ((ByteLinkedOpenHashSet) current).clone()));
        register(ByteAVLTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new ByteAVLTreeSet() : ((ByteAVLTreeSet) current).clone()));
        register(ByteRBTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new ByteRBTreeSet() : ((ByteRBTreeSet) current).clone()));
        register(ByteBigArrayBigList.class, (type, current) -> new Construction(
                () -> current == null ? new ByteBigArrayBigList() : ((ByteBigArrayBigList) current).clone()));
        register(ByteOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((ByteOpenCustomHashSet) current).clone()));
        register(ByteLinkedOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((ByteLinkedOpenCustomHashSet) current).clone()));
        register(ByteCollections.unmodifiable(new ByteArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ByteArrayList(),
                        decoded -> ByteCollections.unmodifiable((ByteCollection) decoded)));
        register(ByteCollections.unmodifiable(new ByteArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ByteArrayList(),
                        decoded -> ByteCollections.unmodifiable((ByteCollection) decoded)));
        register(ByteCollections.synchronize(new ByteArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ByteArrayList(),
                        decoded -> ByteCollections.synchronize((ByteCollection) decoded)));
        register(ByteLists.UnmodifiableList.class, (type, current) -> new Construction(() -> new ByteArrayList(),
                decoded -> ByteLists.unmodifiable((ByteList) decoded)));
        register(ByteLists.EmptyList.class, (type, current) -> new Construction(() -> new ByteArrayList(),
                decoded -> ByteLists.unmodifiable((ByteList) decoded)));
        register(ByteLists.Singleton.class, (type, current) -> new Construction(() -> new ByteArrayList(),
                decoded -> ByteLists.unmodifiable((ByteList) decoded)));
        register(ByteLists.SynchronizedList.class, (type, current) -> new Construction(() -> new ByteArrayList(),
                decoded -> ByteLists.synchronize((ByteList) decoded)));
        register(ByteLists.UnmodifiableRandomAccessList.class,
                (type, current) -> new Construction(() -> new ByteArrayList(),
                        decoded -> ByteLists.unmodifiable((ByteList) decoded)));
        register(ByteLists.SynchronizedRandomAccessList.class,
                (type, current) -> new Construction(() -> new ByteArrayList(),
                        decoded -> ByteLists.synchronize((ByteList) decoded)));
        register(ByteSets.UnmodifiableSet.class, (type, current) -> new Construction(() -> new ByteOpenHashSet(),
                decoded -> ByteSets.unmodifiable((ByteSet) decoded)));
        register(ByteSets.EmptySet.class, (type, current) -> new Construction(() -> new ByteOpenHashSet(),
                decoded -> ByteSets.unmodifiable((ByteSet) decoded)));
        register(ByteSets.Singleton.class, (type, current) -> new Construction(() -> new ByteOpenHashSet(),
                decoded -> ByteSets.unmodifiable((ByteSet) decoded)));
        register(ByteSets.SynchronizedSet.class, (type, current) -> new Construction(() -> new ByteOpenHashSet(),
                decoded -> ByteSets.synchronize((ByteSet) decoded)));
        register(ByteSortedSets.UnmodifiableSortedSet.class,
                (type, current) -> new Construction(
                        () -> new ByteRBTreeSet(current == null ? null : ((ByteSortedSet) current).comparator()),
                        decoded -> ByteSortedSets.unmodifiable((ByteSortedSet) decoded)));
        register(ByteSortedSets.EmptySet.class,
                (type, current) -> new Construction(
                        () -> new ByteRBTreeSet(current == null ? null : ((ByteSortedSet) current).comparator()),
                        decoded -> ByteSortedSets.unmodifiable((ByteSortedSet) decoded)));
        register(ByteSortedSets.Singleton.class,
                (type, current) -> new Construction(
                        () -> new ByteRBTreeSet(current == null ? null : ((ByteSortedSet) current).comparator()),
                        decoded -> ByteSortedSets.unmodifiable((ByteSortedSet) decoded)));
        register(ByteSortedSets.SynchronizedSortedSet.class,
                (type, current) -> new Construction(
                        () -> new ByteRBTreeSet(current == null ? null : ((ByteSortedSet) current).comparator()),
                        decoded -> ByteSortedSets.synchronize((ByteSortedSet) decoded)));
        register(ByteBigLists.UnmodifiableBigList.class,
                (type, current) -> new Construction(() -> new ByteBigArrayBigList(),
                        decoded -> ByteBigLists.unmodifiable((ByteBigList) decoded)));
        register(ByteBigLists.EmptyBigList.class, (type, current) -> new Construction(() -> new ByteBigArrayBigList(),
                decoded -> ByteBigLists.unmodifiable((ByteBigList) decoded)));
        register(ByteBigLists.Singleton.class, (type, current) -> new Construction(() -> new ByteBigArrayBigList(),
                decoded -> ByteBigLists.unmodifiable((ByteBigList) decoded)));
        register(ByteBigLists.SynchronizedBigList.class,
                (type, current) -> new Construction(() -> new ByteBigArrayBigList(),
                        decoded -> ByteBigLists.synchronize((ByteBigList) decoded)));
        register(ByteImmutableList.class, (type, current) -> new Construction(() -> new ByteArrayList(),
                decoded -> new ByteImmutableList((Collection) decoded)));
        register(new ByteImmutableList(new byte[] { 0, 1 }).subList(0, 1).getClass(),
                (type, current) -> new Construction(() -> new ByteArrayList(),
                        decoded -> new ByteImmutableList((Collection) decoded)));
        register(ByteMappedBigList.class, (type, current) -> new Construction(() -> new ByteBigArrayBigList(),
                decoded -> updateMapped((BigList) current, (BigList) decoded)));
    }

    private static void registerShort() {
        register(Short2BooleanMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2BooleanMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2BooleanOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2BooleanOpenHashMap() : ((Short2BooleanOpenHashMap) current).clone()));
        register(Short2BooleanArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2BooleanArrayMap() : ((Short2BooleanArrayMap) current).clone()));
        register(Short2BooleanLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2BooleanLinkedOpenHashMap() : ((Short2BooleanLinkedOpenHashMap) current).clone()));
        register(Short2BooleanAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2BooleanAVLTreeMap() : ((Short2BooleanAVLTreeMap) current).clone()));
        register(Short2BooleanRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2BooleanRBTreeMap() : ((Short2BooleanRBTreeMap) current).clone()));
        register(Short2BooleanOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Short2BooleanOpenCustomHashMap) current).clone()));
        register(Short2BooleanSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2BooleanAVLTreeMap();
            var result = new Short2BooleanAVLTreeMap(((Short2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Short2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2BooleanSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2BooleanAVLTreeMap();
            var result = new Short2BooleanAVLTreeMap(((Short2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Short2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2BooleanSortedMaps.unmodifiable((Short2BooleanSortedMap) decoded)));
        register(Short2BooleanSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2BooleanAVLTreeMap();
            var result = new Short2BooleanAVLTreeMap(((Short2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Short2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2BooleanSortedMaps.unmodifiable((Short2BooleanSortedMap) decoded)));
        register(Short2BooleanSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2BooleanAVLTreeMap();
            var result = new Short2BooleanAVLTreeMap(((Short2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Short2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2BooleanSortedMaps.unmodifiable((Short2BooleanSortedMap) decoded)));
        register(Short2BooleanSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2BooleanAVLTreeMap();
            var result = new Short2BooleanAVLTreeMap(((Short2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Short2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2BooleanSortedMaps.synchronize((Short2BooleanSortedMap) decoded)));
        register(Short2BooleanMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2BooleanMaps.unmodifiable((Short2BooleanMap) decoded)));
        register(Short2BooleanMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2BooleanMaps.unmodifiable((Short2BooleanMap) decoded)));
        register(Short2BooleanMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Short2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2BooleanMaps.unmodifiable((Short2BooleanMap) decoded)));
        register(Short2BooleanMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2BooleanMaps.synchronize((Short2BooleanMap) decoded)));
        register(Short2ByteMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ByteMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2ByteOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ByteOpenHashMap() : ((Short2ByteOpenHashMap) current).clone()));
        register(Short2ByteArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ByteArrayMap() : ((Short2ByteArrayMap) current).clone()));
        register(Short2ByteLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2ByteLinkedOpenHashMap() : ((Short2ByteLinkedOpenHashMap) current).clone()));
        register(Short2ByteAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ByteAVLTreeMap() : ((Short2ByteAVLTreeMap) current).clone()));
        register(Short2ByteRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ByteRBTreeMap() : ((Short2ByteRBTreeMap) current).clone()));
        register(Short2ByteOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Short2ByteOpenCustomHashMap) current).clone()));
        register(Short2ByteSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ByteAVLTreeMap();
            var result = new Short2ByteAVLTreeMap(((Short2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ByteSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2ByteSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ByteAVLTreeMap();
            var result = new Short2ByteAVLTreeMap(((Short2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ByteSortedMaps.unmodifiable((Short2ByteSortedMap) decoded)));
        register(Short2ByteSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ByteAVLTreeMap();
            var result = new Short2ByteAVLTreeMap(((Short2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ByteSortedMaps.unmodifiable((Short2ByteSortedMap) decoded)));
        register(Short2ByteSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ByteAVLTreeMap();
            var result = new Short2ByteAVLTreeMap(((Short2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ByteSortedMaps.unmodifiable((Short2ByteSortedMap) decoded)));
        register(Short2ByteSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ByteAVLTreeMap();
            var result = new Short2ByteAVLTreeMap(((Short2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ByteSortedMaps.synchronize((Short2ByteSortedMap) decoded)));
        register(Short2ByteMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ByteMaps.unmodifiable((Short2ByteMap) decoded)));
        register(Short2ByteMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ByteMaps.unmodifiable((Short2ByteMap) decoded)));
        register(Short2ByteMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Short2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ByteMaps.unmodifiable((Short2ByteMap) decoded)));
        register(Short2ByteMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ByteMaps.synchronize((Short2ByteMap) decoded)));
        register(Short2ShortMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ShortMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2ShortOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ShortOpenHashMap() : ((Short2ShortOpenHashMap) current).clone()));
        register(Short2ShortArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ShortArrayMap() : ((Short2ShortArrayMap) current).clone()));
        register(Short2ShortLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2ShortLinkedOpenHashMap() : ((Short2ShortLinkedOpenHashMap) current).clone()));
        register(Short2ShortAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ShortAVLTreeMap() : ((Short2ShortAVLTreeMap) current).clone()));
        register(Short2ShortRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ShortRBTreeMap() : ((Short2ShortRBTreeMap) current).clone()));
        register(Short2ShortOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Short2ShortOpenCustomHashMap) current).clone()));
        register(Short2ShortSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ShortAVLTreeMap();
            var result = new Short2ShortAVLTreeMap(((Short2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ShortSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2ShortSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ShortAVLTreeMap();
            var result = new Short2ShortAVLTreeMap(((Short2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ShortSortedMaps.unmodifiable((Short2ShortSortedMap) decoded)));
        register(Short2ShortSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ShortAVLTreeMap();
            var result = new Short2ShortAVLTreeMap(((Short2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ShortSortedMaps.unmodifiable((Short2ShortSortedMap) decoded)));
        register(Short2ShortSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ShortAVLTreeMap();
            var result = new Short2ShortAVLTreeMap(((Short2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ShortSortedMaps.unmodifiable((Short2ShortSortedMap) decoded)));
        register(Short2ShortSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ShortAVLTreeMap();
            var result = new Short2ShortAVLTreeMap(((Short2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ShortSortedMaps.synchronize((Short2ShortSortedMap) decoded)));
        register(Short2ShortMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ShortMaps.unmodifiable((Short2ShortMap) decoded)));
        register(Short2ShortMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ShortMaps.unmodifiable((Short2ShortMap) decoded)));
        register(Short2ShortMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Short2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ShortMaps.unmodifiable((Short2ShortMap) decoded)));
        register(Short2ShortMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ShortMaps.synchronize((Short2ShortMap) decoded)));
        register(Short2CharMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2CharMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2CharOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2CharOpenHashMap() : ((Short2CharOpenHashMap) current).clone()));
        register(Short2CharArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2CharArrayMap() : ((Short2CharArrayMap) current).clone()));
        register(Short2CharLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2CharLinkedOpenHashMap() : ((Short2CharLinkedOpenHashMap) current).clone()));
        register(Short2CharAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2CharAVLTreeMap() : ((Short2CharAVLTreeMap) current).clone()));
        register(Short2CharRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2CharRBTreeMap() : ((Short2CharRBTreeMap) current).clone()));
        register(Short2CharOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Short2CharOpenCustomHashMap) current).clone()));
        register(Short2CharSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2CharAVLTreeMap();
            var result = new Short2CharAVLTreeMap(((Short2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Short2CharSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2CharSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2CharAVLTreeMap();
            var result = new Short2CharAVLTreeMap(((Short2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Short2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2CharSortedMaps.unmodifiable((Short2CharSortedMap) decoded)));
        register(Short2CharSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2CharAVLTreeMap();
            var result = new Short2CharAVLTreeMap(((Short2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Short2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2CharSortedMaps.unmodifiable((Short2CharSortedMap) decoded)));
        register(Short2CharSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2CharAVLTreeMap();
            var result = new Short2CharAVLTreeMap(((Short2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Short2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2CharSortedMaps.unmodifiable((Short2CharSortedMap) decoded)));
        register(Short2CharSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2CharAVLTreeMap();
            var result = new Short2CharAVLTreeMap(((Short2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Short2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2CharSortedMaps.synchronize((Short2CharSortedMap) decoded)));
        register(Short2CharMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2CharMaps.unmodifiable((Short2CharMap) decoded)));
        register(Short2CharMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2CharMaps.unmodifiable((Short2CharMap) decoded)));
        register(Short2CharMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Short2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2CharMaps.unmodifiable((Short2CharMap) decoded)));
        register(Short2CharMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2CharMaps.synchronize((Short2CharMap) decoded)));
        register(Short2IntMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2IntMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2IntOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2IntOpenHashMap() : ((Short2IntOpenHashMap) current).clone()));
        register(Short2IntArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2IntArrayMap() : ((Short2IntArrayMap) current).clone()));
        register(Short2IntLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2IntLinkedOpenHashMap() : ((Short2IntLinkedOpenHashMap) current).clone()));
        register(Short2IntAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2IntAVLTreeMap() : ((Short2IntAVLTreeMap) current).clone()));
        register(Short2IntRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2IntRBTreeMap() : ((Short2IntRBTreeMap) current).clone()));
        register(Short2IntOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Short2IntOpenCustomHashMap) current).clone()));
        register(Short2IntSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2IntAVLTreeMap();
            var result = new Short2IntAVLTreeMap(((Short2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Short2IntSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2IntSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2IntAVLTreeMap();
            var result = new Short2IntAVLTreeMap(((Short2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Short2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2IntSortedMaps.unmodifiable((Short2IntSortedMap) decoded)));
        register(Short2IntSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2IntAVLTreeMap();
            var result = new Short2IntAVLTreeMap(((Short2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Short2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2IntSortedMaps.unmodifiable((Short2IntSortedMap) decoded)));
        register(Short2IntSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2IntAVLTreeMap();
            var result = new Short2IntAVLTreeMap(((Short2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Short2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2IntSortedMaps.unmodifiable((Short2IntSortedMap) decoded)));
        register(Short2IntSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2IntAVLTreeMap();
            var result = new Short2IntAVLTreeMap(((Short2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Short2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2IntSortedMaps.synchronize((Short2IntSortedMap) decoded)));
        register(Short2IntMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2IntMaps.unmodifiable((Short2IntMap) decoded)));
        register(Short2IntMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2IntMaps.unmodifiable((Short2IntMap) decoded)));
        register(Short2IntMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Short2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2IntMaps.unmodifiable((Short2IntMap) decoded)));
        register(Short2IntMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2IntMaps.synchronize((Short2IntMap) decoded)));
        register(Short2LongMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2LongMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2LongOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2LongOpenHashMap() : ((Short2LongOpenHashMap) current).clone()));
        register(Short2LongArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2LongArrayMap() : ((Short2LongArrayMap) current).clone()));
        register(Short2LongLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2LongLinkedOpenHashMap() : ((Short2LongLinkedOpenHashMap) current).clone()));
        register(Short2LongAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2LongAVLTreeMap() : ((Short2LongAVLTreeMap) current).clone()));
        register(Short2LongRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2LongRBTreeMap() : ((Short2LongRBTreeMap) current).clone()));
        register(Short2LongOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Short2LongOpenCustomHashMap) current).clone()));
        register(Short2LongSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2LongAVLTreeMap();
            var result = new Short2LongAVLTreeMap(((Short2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Short2LongSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2LongSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2LongAVLTreeMap();
            var result = new Short2LongAVLTreeMap(((Short2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Short2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2LongSortedMaps.unmodifiable((Short2LongSortedMap) decoded)));
        register(Short2LongSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2LongAVLTreeMap();
            var result = new Short2LongAVLTreeMap(((Short2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Short2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2LongSortedMaps.unmodifiable((Short2LongSortedMap) decoded)));
        register(Short2LongSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2LongAVLTreeMap();
            var result = new Short2LongAVLTreeMap(((Short2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Short2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2LongSortedMaps.unmodifiable((Short2LongSortedMap) decoded)));
        register(Short2LongSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2LongAVLTreeMap();
            var result = new Short2LongAVLTreeMap(((Short2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Short2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2LongSortedMaps.synchronize((Short2LongSortedMap) decoded)));
        register(Short2LongMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2LongMaps.unmodifiable((Short2LongMap) decoded)));
        register(Short2LongMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2LongMaps.unmodifiable((Short2LongMap) decoded)));
        register(Short2LongMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Short2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2LongMaps.unmodifiable((Short2LongMap) decoded)));
        register(Short2LongMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2LongMaps.synchronize((Short2LongMap) decoded)));
        register(Short2FloatMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2FloatMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2FloatOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2FloatOpenHashMap() : ((Short2FloatOpenHashMap) current).clone()));
        register(Short2FloatArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2FloatArrayMap() : ((Short2FloatArrayMap) current).clone()));
        register(Short2FloatLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2FloatLinkedOpenHashMap() : ((Short2FloatLinkedOpenHashMap) current).clone()));
        register(Short2FloatAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2FloatAVLTreeMap() : ((Short2FloatAVLTreeMap) current).clone()));
        register(Short2FloatRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2FloatRBTreeMap() : ((Short2FloatRBTreeMap) current).clone()));
        register(Short2FloatOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Short2FloatOpenCustomHashMap) current).clone()));
        register(Short2FloatSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2FloatAVLTreeMap();
            var result = new Short2FloatAVLTreeMap(((Short2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Short2FloatSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2FloatSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2FloatAVLTreeMap();
            var result = new Short2FloatAVLTreeMap(((Short2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Short2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2FloatSortedMaps.unmodifiable((Short2FloatSortedMap) decoded)));
        register(Short2FloatSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2FloatAVLTreeMap();
            var result = new Short2FloatAVLTreeMap(((Short2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Short2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2FloatSortedMaps.unmodifiable((Short2FloatSortedMap) decoded)));
        register(Short2FloatSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2FloatAVLTreeMap();
            var result = new Short2FloatAVLTreeMap(((Short2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Short2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2FloatSortedMaps.unmodifiable((Short2FloatSortedMap) decoded)));
        register(Short2FloatSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2FloatAVLTreeMap();
            var result = new Short2FloatAVLTreeMap(((Short2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Short2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2FloatSortedMaps.synchronize((Short2FloatSortedMap) decoded)));
        register(Short2FloatMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2FloatMaps.unmodifiable((Short2FloatMap) decoded)));
        register(Short2FloatMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2FloatMaps.unmodifiable((Short2FloatMap) decoded)));
        register(Short2FloatMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Short2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2FloatMaps.unmodifiable((Short2FloatMap) decoded)));
        register(Short2FloatMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2FloatMaps.synchronize((Short2FloatMap) decoded)));
        register(Short2DoubleMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2DoubleMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2DoubleOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2DoubleOpenHashMap() : ((Short2DoubleOpenHashMap) current).clone()));
        register(Short2DoubleArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2DoubleArrayMap() : ((Short2DoubleArrayMap) current).clone()));
        register(Short2DoubleLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2DoubleLinkedOpenHashMap() : ((Short2DoubleLinkedOpenHashMap) current).clone()));
        register(Short2DoubleAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2DoubleAVLTreeMap() : ((Short2DoubleAVLTreeMap) current).clone()));
        register(Short2DoubleRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2DoubleRBTreeMap() : ((Short2DoubleRBTreeMap) current).clone()));
        register(Short2DoubleOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Short2DoubleOpenCustomHashMap) current).clone()));
        register(Short2DoubleSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2DoubleAVLTreeMap();
            var result = new Short2DoubleAVLTreeMap(((Short2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Short2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2DoubleSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2DoubleAVLTreeMap();
            var result = new Short2DoubleAVLTreeMap(((Short2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Short2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2DoubleSortedMaps.unmodifiable((Short2DoubleSortedMap) decoded)));
        register(Short2DoubleSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2DoubleAVLTreeMap();
            var result = new Short2DoubleAVLTreeMap(((Short2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Short2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2DoubleSortedMaps.unmodifiable((Short2DoubleSortedMap) decoded)));
        register(Short2DoubleSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2DoubleAVLTreeMap();
            var result = new Short2DoubleAVLTreeMap(((Short2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Short2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2DoubleSortedMaps.unmodifiable((Short2DoubleSortedMap) decoded)));
        register(Short2DoubleSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2DoubleAVLTreeMap();
            var result = new Short2DoubleAVLTreeMap(((Short2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Short2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2DoubleSortedMaps.synchronize((Short2DoubleSortedMap) decoded)));
        register(Short2DoubleMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2DoubleMaps.unmodifiable((Short2DoubleMap) decoded)));
        register(Short2DoubleMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2DoubleMaps.unmodifiable((Short2DoubleMap) decoded)));
        register(Short2DoubleMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Short2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2DoubleMaps.unmodifiable((Short2DoubleMap) decoded)));
        register(Short2DoubleMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2DoubleMaps.synchronize((Short2DoubleMap) decoded)));
        register(Short2ObjectMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ObjectMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2ObjectOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ObjectOpenHashMap() : ((Short2ObjectOpenHashMap) current).clone()));
        register(Short2ObjectArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ObjectArrayMap() : ((Short2ObjectArrayMap) current).clone()));
        register(Short2ObjectLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2ObjectLinkedOpenHashMap() : ((Short2ObjectLinkedOpenHashMap) current).clone()));
        register(Short2ObjectAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ObjectAVLTreeMap() : ((Short2ObjectAVLTreeMap) current).clone()));
        register(Short2ObjectRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ObjectRBTreeMap() : ((Short2ObjectRBTreeMap) current).clone()));
        register(Short2ObjectOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Short2ObjectOpenCustomHashMap) current).clone()));
        register(Short2ObjectSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ObjectAVLTreeMap();
            var result = new Short2ObjectAVLTreeMap(((Short2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2ObjectSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ObjectAVLTreeMap();
            var result = new Short2ObjectAVLTreeMap(((Short2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ObjectSortedMaps.unmodifiable((Short2ObjectSortedMap) decoded)));
        register(Short2ObjectSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ObjectAVLTreeMap();
            var result = new Short2ObjectAVLTreeMap(((Short2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ObjectSortedMaps.unmodifiable((Short2ObjectSortedMap) decoded)));
        register(Short2ObjectSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ObjectAVLTreeMap();
            var result = new Short2ObjectAVLTreeMap(((Short2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ObjectSortedMaps.unmodifiable((Short2ObjectSortedMap) decoded)));
        register(Short2ObjectSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ObjectAVLTreeMap();
            var result = new Short2ObjectAVLTreeMap(((Short2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ObjectSortedMaps.synchronize((Short2ObjectSortedMap) decoded)));
        register(Short2ObjectMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ObjectMaps.unmodifiable((Short2ObjectMap) decoded)));
        register(Short2ObjectMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ObjectMaps.unmodifiable((Short2ObjectMap) decoded)));
        register(Short2ObjectMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Short2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ObjectMaps.unmodifiable((Short2ObjectMap) decoded)));
        register(Short2ObjectMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ObjectMaps.synchronize((Short2ObjectMap) decoded)));
        register(Short2ReferenceMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ReferenceMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2ReferenceOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2ReferenceOpenHashMap() : ((Short2ReferenceOpenHashMap) current).clone()));
        register(Short2ReferenceArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ReferenceArrayMap() : ((Short2ReferenceArrayMap) current).clone()));
        register(Short2ReferenceLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2ReferenceLinkedOpenHashMap() : ((Short2ReferenceLinkedOpenHashMap) current).clone()));
        register(Short2ReferenceAVLTreeMap.class, (type, current) -> new Construction(() -> current == null ?
                new Short2ReferenceAVLTreeMap() : ((Short2ReferenceAVLTreeMap) current).clone()));
        register(Short2ReferenceRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Short2ReferenceRBTreeMap() : ((Short2ReferenceRBTreeMap) current).clone()));
        register(Short2ReferenceOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Short2ReferenceOpenCustomHashMap) current).clone()));
        register(Short2ReferenceSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ReferenceAVLTreeMap();
            var result = new Short2ReferenceAVLTreeMap(((Short2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Short2ReferenceSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ReferenceAVLTreeMap();
            var result = new Short2ReferenceAVLTreeMap(((Short2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ReferenceSortedMaps.unmodifiable((Short2ReferenceSortedMap) decoded)));
        register(Short2ReferenceSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ReferenceAVLTreeMap();
            var result = new Short2ReferenceAVLTreeMap(((Short2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ReferenceSortedMaps.unmodifiable((Short2ReferenceSortedMap) decoded)));
        register(Short2ReferenceSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ReferenceAVLTreeMap();
            var result = new Short2ReferenceAVLTreeMap(((Short2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ReferenceSortedMaps.unmodifiable((Short2ReferenceSortedMap) decoded)));
        register(Short2ReferenceSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Short2ReferenceAVLTreeMap();
            var result = new Short2ReferenceAVLTreeMap(((Short2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Short2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ReferenceSortedMaps.synchronize((Short2ReferenceSortedMap) decoded)));
        register(Short2ReferenceMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ReferenceMaps.unmodifiable((Short2ReferenceMap) decoded)));
        register(Short2ReferenceMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ReferenceMaps.unmodifiable((Short2ReferenceMap) decoded)));
        register(Short2ReferenceMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Short2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ReferenceMaps.unmodifiable((Short2ReferenceMap) decoded)));
        register(Short2ReferenceMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Short2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Short2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Short2ReferenceMaps.synchronize((Short2ReferenceMap) decoded)));
        register(ShortCollection.class, (type, current) -> new Construction(() -> new ShortArrayList()));
        register(ShortList.class, (type, current) -> new Construction(() -> new ShortArrayList()));
        register(ShortSet.class, (type, current) -> new Construction(() -> new ShortOpenHashSet()));
        register(ShortSortedSet.class, (type, current) -> new Construction(() -> current == null ?
                new ShortRBTreeSet() : new ShortRBTreeSet(((ShortSortedSet) current).comparator())));
        register(ShortBigList.class, (type, current) -> new Construction(() -> new ShortBigArrayBigList()));
        register(ShortArrayList.class, (type, current) -> new Construction(
                () -> current == null ? new ShortArrayList() : ((ShortArrayList) current).clone()));
        register(ShortArraySet.class, (type, current) -> new Construction(
                () -> current == null ? new ShortArraySet() : ((ShortArraySet) current).clone()));
        register(ShortOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new ShortOpenHashSet() : ((ShortOpenHashSet) current).clone()));
        register(ShortLinkedOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new ShortLinkedOpenHashSet() : ((ShortLinkedOpenHashSet) current).clone()));
        register(ShortAVLTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new ShortAVLTreeSet() : ((ShortAVLTreeSet) current).clone()));
        register(ShortRBTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new ShortRBTreeSet() : ((ShortRBTreeSet) current).clone()));
        register(ShortBigArrayBigList.class, (type, current) -> new Construction(
                () -> current == null ? new ShortBigArrayBigList() : ((ShortBigArrayBigList) current).clone()));
        register(ShortOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((ShortOpenCustomHashSet) current).clone()));
        register(ShortLinkedOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((ShortLinkedOpenCustomHashSet) current).clone()));
        register(ShortCollections.unmodifiable(new ShortArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ShortArrayList(),
                        decoded -> ShortCollections.unmodifiable((ShortCollection) decoded)));
        register(ShortCollections.unmodifiable(new ShortArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ShortArrayList(),
                        decoded -> ShortCollections.unmodifiable((ShortCollection) decoded)));
        register(ShortCollections.synchronize(new ShortArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ShortArrayList(),
                        decoded -> ShortCollections.synchronize((ShortCollection) decoded)));
        register(ShortLists.UnmodifiableList.class, (type, current) -> new Construction(() -> new ShortArrayList(),
                decoded -> ShortLists.unmodifiable((ShortList) decoded)));
        register(ShortLists.EmptyList.class, (type, current) -> new Construction(() -> new ShortArrayList(),
                decoded -> ShortLists.unmodifiable((ShortList) decoded)));
        register(ShortLists.Singleton.class, (type, current) -> new Construction(() -> new ShortArrayList(),
                decoded -> ShortLists.unmodifiable((ShortList) decoded)));
        register(ShortLists.SynchronizedList.class, (type, current) -> new Construction(() -> new ShortArrayList(),
                decoded -> ShortLists.synchronize((ShortList) decoded)));
        register(ShortLists.UnmodifiableRandomAccessList.class,
                (type, current) -> new Construction(() -> new ShortArrayList(),
                        decoded -> ShortLists.unmodifiable((ShortList) decoded)));
        register(ShortLists.SynchronizedRandomAccessList.class,
                (type, current) -> new Construction(() -> new ShortArrayList(),
                        decoded -> ShortLists.synchronize((ShortList) decoded)));
        register(ShortSets.UnmodifiableSet.class, (type, current) -> new Construction(() -> new ShortOpenHashSet(),
                decoded -> ShortSets.unmodifiable((ShortSet) decoded)));
        register(ShortSets.EmptySet.class, (type, current) -> new Construction(() -> new ShortOpenHashSet(),
                decoded -> ShortSets.unmodifiable((ShortSet) decoded)));
        register(ShortSets.Singleton.class, (type, current) -> new Construction(() -> new ShortOpenHashSet(),
                decoded -> ShortSets.unmodifiable((ShortSet) decoded)));
        register(ShortSets.SynchronizedSet.class, (type, current) -> new Construction(() -> new ShortOpenHashSet(),
                decoded -> ShortSets.synchronize((ShortSet) decoded)));
        register(ShortSortedSets.UnmodifiableSortedSet.class,
                (type, current) -> new Construction(
                        () -> new ShortRBTreeSet(current == null ? null : ((ShortSortedSet) current).comparator()),
                        decoded -> ShortSortedSets.unmodifiable((ShortSortedSet) decoded)));
        register(ShortSortedSets.EmptySet.class,
                (type, current) -> new Construction(
                        () -> new ShortRBTreeSet(current == null ? null : ((ShortSortedSet) current).comparator()),
                        decoded -> ShortSortedSets.unmodifiable((ShortSortedSet) decoded)));
        register(ShortSortedSets.Singleton.class,
                (type, current) -> new Construction(
                        () -> new ShortRBTreeSet(current == null ? null : ((ShortSortedSet) current).comparator()),
                        decoded -> ShortSortedSets.unmodifiable((ShortSortedSet) decoded)));
        register(ShortSortedSets.SynchronizedSortedSet.class,
                (type, current) -> new Construction(
                        () -> new ShortRBTreeSet(current == null ? null : ((ShortSortedSet) current).comparator()),
                        decoded -> ShortSortedSets.synchronize((ShortSortedSet) decoded)));
        register(ShortBigLists.UnmodifiableBigList.class,
                (type, current) -> new Construction(() -> new ShortBigArrayBigList(),
                        decoded -> ShortBigLists.unmodifiable((ShortBigList) decoded)));
        register(ShortBigLists.EmptyBigList.class, (type, current) -> new Construction(() -> new ShortBigArrayBigList(),
                decoded -> ShortBigLists.unmodifiable((ShortBigList) decoded)));
        register(ShortBigLists.Singleton.class, (type, current) -> new Construction(() -> new ShortBigArrayBigList(),
                decoded -> ShortBigLists.unmodifiable((ShortBigList) decoded)));
        register(ShortBigLists.SynchronizedBigList.class,
                (type, current) -> new Construction(() -> new ShortBigArrayBigList(),
                        decoded -> ShortBigLists.synchronize((ShortBigList) decoded)));
        register(ShortImmutableList.class, (type, current) -> new Construction(() -> new ShortArrayList(),
                decoded -> new ShortImmutableList((Collection) decoded)));
        register(new ShortImmutableList(new short[] { 0, 1 }).subList(0, 1).getClass(),
                (type, current) -> new Construction(() -> new ShortArrayList(),
                        decoded -> new ShortImmutableList((Collection) decoded)));
        register(ShortMappedBigList.class, (type, current) -> new Construction(() -> new ShortBigArrayBigList(),
                decoded -> updateMapped((BigList) current, (BigList) decoded)));
    }

    private static void registerChar() {
        register(Char2BooleanMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2BooleanMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2BooleanOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2BooleanOpenHashMap() : ((Char2BooleanOpenHashMap) current).clone()));
        register(Char2BooleanArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2BooleanArrayMap() : ((Char2BooleanArrayMap) current).clone()));
        register(Char2BooleanLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Char2BooleanLinkedOpenHashMap() : ((Char2BooleanLinkedOpenHashMap) current).clone()));
        register(Char2BooleanAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2BooleanAVLTreeMap() : ((Char2BooleanAVLTreeMap) current).clone()));
        register(Char2BooleanRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2BooleanRBTreeMap() : ((Char2BooleanRBTreeMap) current).clone()));
        register(Char2BooleanOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Char2BooleanOpenCustomHashMap) current).clone()));
        register(Char2BooleanSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2BooleanAVLTreeMap();
            var result = new Char2BooleanAVLTreeMap(((Char2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Char2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2BooleanSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2BooleanAVLTreeMap();
            var result = new Char2BooleanAVLTreeMap(((Char2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Char2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2BooleanSortedMaps.unmodifiable((Char2BooleanSortedMap) decoded)));
        register(Char2BooleanSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2BooleanAVLTreeMap();
            var result = new Char2BooleanAVLTreeMap(((Char2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Char2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2BooleanSortedMaps.unmodifiable((Char2BooleanSortedMap) decoded)));
        register(Char2BooleanSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2BooleanAVLTreeMap();
            var result = new Char2BooleanAVLTreeMap(((Char2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Char2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2BooleanSortedMaps.unmodifiable((Char2BooleanSortedMap) decoded)));
        register(Char2BooleanSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2BooleanAVLTreeMap();
            var result = new Char2BooleanAVLTreeMap(((Char2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Char2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2BooleanSortedMaps.synchronize((Char2BooleanSortedMap) decoded)));
        register(Char2BooleanMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2BooleanMaps.unmodifiable((Char2BooleanMap) decoded)));
        register(Char2BooleanMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2BooleanMaps.unmodifiable((Char2BooleanMap) decoded)));
        register(Char2BooleanMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Char2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2BooleanMaps.unmodifiable((Char2BooleanMap) decoded)));
        register(Char2BooleanMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2BooleanMaps.synchronize((Char2BooleanMap) decoded)));
        register(Char2ByteMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ByteMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2ByteOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ByteOpenHashMap() : ((Char2ByteOpenHashMap) current).clone()));
        register(Char2ByteArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ByteArrayMap() : ((Char2ByteArrayMap) current).clone()));
        register(Char2ByteLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Char2ByteLinkedOpenHashMap() : ((Char2ByteLinkedOpenHashMap) current).clone()));
        register(Char2ByteAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ByteAVLTreeMap() : ((Char2ByteAVLTreeMap) current).clone()));
        register(Char2ByteRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ByteRBTreeMap() : ((Char2ByteRBTreeMap) current).clone()));
        register(Char2ByteOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Char2ByteOpenCustomHashMap) current).clone()));
        register(Char2ByteSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ByteAVLTreeMap();
            var result = new Char2ByteAVLTreeMap(((Char2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ByteSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2ByteSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ByteAVLTreeMap();
            var result = new Char2ByteAVLTreeMap(((Char2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ByteSortedMaps.unmodifiable((Char2ByteSortedMap) decoded)));
        register(Char2ByteSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ByteAVLTreeMap();
            var result = new Char2ByteAVLTreeMap(((Char2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ByteSortedMaps.unmodifiable((Char2ByteSortedMap) decoded)));
        register(Char2ByteSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ByteAVLTreeMap();
            var result = new Char2ByteAVLTreeMap(((Char2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ByteSortedMaps.unmodifiable((Char2ByteSortedMap) decoded)));
        register(Char2ByteSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ByteAVLTreeMap();
            var result = new Char2ByteAVLTreeMap(((Char2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ByteSortedMaps.synchronize((Char2ByteSortedMap) decoded)));
        register(Char2ByteMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ByteMaps.unmodifiable((Char2ByteMap) decoded)));
        register(Char2ByteMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ByteMaps.unmodifiable((Char2ByteMap) decoded)));
        register(Char2ByteMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Char2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ByteMaps.unmodifiable((Char2ByteMap) decoded)));
        register(Char2ByteMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ByteMaps.synchronize((Char2ByteMap) decoded)));
        register(Char2ShortMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ShortMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2ShortOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ShortOpenHashMap() : ((Char2ShortOpenHashMap) current).clone()));
        register(Char2ShortArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ShortArrayMap() : ((Char2ShortArrayMap) current).clone()));
        register(Char2ShortLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Char2ShortLinkedOpenHashMap() : ((Char2ShortLinkedOpenHashMap) current).clone()));
        register(Char2ShortAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ShortAVLTreeMap() : ((Char2ShortAVLTreeMap) current).clone()));
        register(Char2ShortRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ShortRBTreeMap() : ((Char2ShortRBTreeMap) current).clone()));
        register(Char2ShortOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Char2ShortOpenCustomHashMap) current).clone()));
        register(Char2ShortSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ShortAVLTreeMap();
            var result = new Char2ShortAVLTreeMap(((Char2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ShortSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2ShortSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ShortAVLTreeMap();
            var result = new Char2ShortAVLTreeMap(((Char2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ShortSortedMaps.unmodifiable((Char2ShortSortedMap) decoded)));
        register(Char2ShortSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ShortAVLTreeMap();
            var result = new Char2ShortAVLTreeMap(((Char2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ShortSortedMaps.unmodifiable((Char2ShortSortedMap) decoded)));
        register(Char2ShortSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ShortAVLTreeMap();
            var result = new Char2ShortAVLTreeMap(((Char2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ShortSortedMaps.unmodifiable((Char2ShortSortedMap) decoded)));
        register(Char2ShortSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ShortAVLTreeMap();
            var result = new Char2ShortAVLTreeMap(((Char2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ShortSortedMaps.synchronize((Char2ShortSortedMap) decoded)));
        register(Char2ShortMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ShortMaps.unmodifiable((Char2ShortMap) decoded)));
        register(Char2ShortMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ShortMaps.unmodifiable((Char2ShortMap) decoded)));
        register(Char2ShortMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Char2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ShortMaps.unmodifiable((Char2ShortMap) decoded)));
        register(Char2ShortMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ShortMaps.synchronize((Char2ShortMap) decoded)));
        register(Char2CharMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2CharMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2CharOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2CharOpenHashMap() : ((Char2CharOpenHashMap) current).clone()));
        register(Char2CharArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2CharArrayMap() : ((Char2CharArrayMap) current).clone()));
        register(Char2CharLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Char2CharLinkedOpenHashMap() : ((Char2CharLinkedOpenHashMap) current).clone()));
        register(Char2CharAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2CharAVLTreeMap() : ((Char2CharAVLTreeMap) current).clone()));
        register(Char2CharRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2CharRBTreeMap() : ((Char2CharRBTreeMap) current).clone()));
        register(Char2CharOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Char2CharOpenCustomHashMap) current).clone()));
        register(Char2CharSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2CharAVLTreeMap();
            var result = new Char2CharAVLTreeMap(((Char2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Char2CharSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2CharSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2CharAVLTreeMap();
            var result = new Char2CharAVLTreeMap(((Char2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Char2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2CharSortedMaps.unmodifiable((Char2CharSortedMap) decoded)));
        register(Char2CharSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2CharAVLTreeMap();
            var result = new Char2CharAVLTreeMap(((Char2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Char2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2CharSortedMaps.unmodifiable((Char2CharSortedMap) decoded)));
        register(Char2CharSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2CharAVLTreeMap();
            var result = new Char2CharAVLTreeMap(((Char2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Char2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2CharSortedMaps.unmodifiable((Char2CharSortedMap) decoded)));
        register(Char2CharSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2CharAVLTreeMap();
            var result = new Char2CharAVLTreeMap(((Char2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Char2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2CharSortedMaps.synchronize((Char2CharSortedMap) decoded)));
        register(Char2CharMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2CharMaps.unmodifiable((Char2CharMap) decoded)));
        register(Char2CharMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2CharMaps.unmodifiable((Char2CharMap) decoded)));
        register(Char2CharMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Char2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2CharMaps.unmodifiable((Char2CharMap) decoded)));
        register(Char2CharMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2CharMaps.synchronize((Char2CharMap) decoded)));
        register(Char2IntMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2IntMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2IntOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2IntOpenHashMap() : ((Char2IntOpenHashMap) current).clone()));
        register(Char2IntArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2IntArrayMap() : ((Char2IntArrayMap) current).clone()));
        register(Char2IntLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Char2IntLinkedOpenHashMap() : ((Char2IntLinkedOpenHashMap) current).clone()));
        register(Char2IntAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2IntAVLTreeMap() : ((Char2IntAVLTreeMap) current).clone()));
        register(Char2IntRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2IntRBTreeMap() : ((Char2IntRBTreeMap) current).clone()));
        register(Char2IntOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Char2IntOpenCustomHashMap) current).clone()));
        register(Char2IntSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2IntAVLTreeMap();
            var result = new Char2IntAVLTreeMap(((Char2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Char2IntSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2IntSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2IntAVLTreeMap();
            var result = new Char2IntAVLTreeMap(((Char2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Char2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2IntSortedMaps.unmodifiable((Char2IntSortedMap) decoded)));
        register(Char2IntSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2IntAVLTreeMap();
            var result = new Char2IntAVLTreeMap(((Char2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Char2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2IntSortedMaps.unmodifiable((Char2IntSortedMap) decoded)));
        register(Char2IntSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2IntAVLTreeMap();
            var result = new Char2IntAVLTreeMap(((Char2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Char2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2IntSortedMaps.unmodifiable((Char2IntSortedMap) decoded)));
        register(Char2IntSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2IntAVLTreeMap();
            var result = new Char2IntAVLTreeMap(((Char2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Char2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2IntSortedMaps.synchronize((Char2IntSortedMap) decoded)));
        register(Char2IntMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2IntMaps.unmodifiable((Char2IntMap) decoded)));
        register(Char2IntMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2IntMaps.unmodifiable((Char2IntMap) decoded)));
        register(Char2IntMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Char2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2IntMaps.unmodifiable((Char2IntMap) decoded)));
        register(Char2IntMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2IntMaps.synchronize((Char2IntMap) decoded)));
        register(Char2LongMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2LongMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2LongOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2LongOpenHashMap() : ((Char2LongOpenHashMap) current).clone()));
        register(Char2LongArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2LongArrayMap() : ((Char2LongArrayMap) current).clone()));
        register(Char2LongLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Char2LongLinkedOpenHashMap() : ((Char2LongLinkedOpenHashMap) current).clone()));
        register(Char2LongAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2LongAVLTreeMap() : ((Char2LongAVLTreeMap) current).clone()));
        register(Char2LongRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2LongRBTreeMap() : ((Char2LongRBTreeMap) current).clone()));
        register(Char2LongOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Char2LongOpenCustomHashMap) current).clone()));
        register(Char2LongSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2LongAVLTreeMap();
            var result = new Char2LongAVLTreeMap(((Char2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Char2LongSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2LongSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2LongAVLTreeMap();
            var result = new Char2LongAVLTreeMap(((Char2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Char2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2LongSortedMaps.unmodifiable((Char2LongSortedMap) decoded)));
        register(Char2LongSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2LongAVLTreeMap();
            var result = new Char2LongAVLTreeMap(((Char2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Char2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2LongSortedMaps.unmodifiable((Char2LongSortedMap) decoded)));
        register(Char2LongSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2LongAVLTreeMap();
            var result = new Char2LongAVLTreeMap(((Char2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Char2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2LongSortedMaps.unmodifiable((Char2LongSortedMap) decoded)));
        register(Char2LongSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2LongAVLTreeMap();
            var result = new Char2LongAVLTreeMap(((Char2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Char2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2LongSortedMaps.synchronize((Char2LongSortedMap) decoded)));
        register(Char2LongMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2LongMaps.unmodifiable((Char2LongMap) decoded)));
        register(Char2LongMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2LongMaps.unmodifiable((Char2LongMap) decoded)));
        register(Char2LongMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Char2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2LongMaps.unmodifiable((Char2LongMap) decoded)));
        register(Char2LongMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2LongMaps.synchronize((Char2LongMap) decoded)));
        register(Char2FloatMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2FloatMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2FloatOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2FloatOpenHashMap() : ((Char2FloatOpenHashMap) current).clone()));
        register(Char2FloatArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2FloatArrayMap() : ((Char2FloatArrayMap) current).clone()));
        register(Char2FloatLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Char2FloatLinkedOpenHashMap() : ((Char2FloatLinkedOpenHashMap) current).clone()));
        register(Char2FloatAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2FloatAVLTreeMap() : ((Char2FloatAVLTreeMap) current).clone()));
        register(Char2FloatRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2FloatRBTreeMap() : ((Char2FloatRBTreeMap) current).clone()));
        register(Char2FloatOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Char2FloatOpenCustomHashMap) current).clone()));
        register(Char2FloatSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2FloatAVLTreeMap();
            var result = new Char2FloatAVLTreeMap(((Char2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Char2FloatSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2FloatSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2FloatAVLTreeMap();
            var result = new Char2FloatAVLTreeMap(((Char2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Char2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2FloatSortedMaps.unmodifiable((Char2FloatSortedMap) decoded)));
        register(Char2FloatSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2FloatAVLTreeMap();
            var result = new Char2FloatAVLTreeMap(((Char2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Char2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2FloatSortedMaps.unmodifiable((Char2FloatSortedMap) decoded)));
        register(Char2FloatSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2FloatAVLTreeMap();
            var result = new Char2FloatAVLTreeMap(((Char2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Char2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2FloatSortedMaps.unmodifiable((Char2FloatSortedMap) decoded)));
        register(Char2FloatSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2FloatAVLTreeMap();
            var result = new Char2FloatAVLTreeMap(((Char2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Char2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2FloatSortedMaps.synchronize((Char2FloatSortedMap) decoded)));
        register(Char2FloatMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2FloatMaps.unmodifiable((Char2FloatMap) decoded)));
        register(Char2FloatMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2FloatMaps.unmodifiable((Char2FloatMap) decoded)));
        register(Char2FloatMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Char2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2FloatMaps.unmodifiable((Char2FloatMap) decoded)));
        register(Char2FloatMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2FloatMaps.synchronize((Char2FloatMap) decoded)));
        register(Char2DoubleMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2DoubleMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2DoubleOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2DoubleOpenHashMap() : ((Char2DoubleOpenHashMap) current).clone()));
        register(Char2DoubleArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2DoubleArrayMap() : ((Char2DoubleArrayMap) current).clone()));
        register(Char2DoubleLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Char2DoubleLinkedOpenHashMap() : ((Char2DoubleLinkedOpenHashMap) current).clone()));
        register(Char2DoubleAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2DoubleAVLTreeMap() : ((Char2DoubleAVLTreeMap) current).clone()));
        register(Char2DoubleRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2DoubleRBTreeMap() : ((Char2DoubleRBTreeMap) current).clone()));
        register(Char2DoubleOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Char2DoubleOpenCustomHashMap) current).clone()));
        register(Char2DoubleSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2DoubleAVLTreeMap();
            var result = new Char2DoubleAVLTreeMap(((Char2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Char2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2DoubleSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2DoubleAVLTreeMap();
            var result = new Char2DoubleAVLTreeMap(((Char2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Char2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2DoubleSortedMaps.unmodifiable((Char2DoubleSortedMap) decoded)));
        register(Char2DoubleSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2DoubleAVLTreeMap();
            var result = new Char2DoubleAVLTreeMap(((Char2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Char2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2DoubleSortedMaps.unmodifiable((Char2DoubleSortedMap) decoded)));
        register(Char2DoubleSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2DoubleAVLTreeMap();
            var result = new Char2DoubleAVLTreeMap(((Char2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Char2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2DoubleSortedMaps.unmodifiable((Char2DoubleSortedMap) decoded)));
        register(Char2DoubleSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2DoubleAVLTreeMap();
            var result = new Char2DoubleAVLTreeMap(((Char2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Char2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2DoubleSortedMaps.synchronize((Char2DoubleSortedMap) decoded)));
        register(Char2DoubleMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2DoubleMaps.unmodifiable((Char2DoubleMap) decoded)));
        register(Char2DoubleMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2DoubleMaps.unmodifiable((Char2DoubleMap) decoded)));
        register(Char2DoubleMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Char2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2DoubleMaps.unmodifiable((Char2DoubleMap) decoded)));
        register(Char2DoubleMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2DoubleMaps.synchronize((Char2DoubleMap) decoded)));
        register(Char2ObjectMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ObjectMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2ObjectOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ObjectOpenHashMap() : ((Char2ObjectOpenHashMap) current).clone()));
        register(Char2ObjectArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ObjectArrayMap() : ((Char2ObjectArrayMap) current).clone()));
        register(Char2ObjectLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Char2ObjectLinkedOpenHashMap() : ((Char2ObjectLinkedOpenHashMap) current).clone()));
        register(Char2ObjectAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ObjectAVLTreeMap() : ((Char2ObjectAVLTreeMap) current).clone()));
        register(Char2ObjectRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ObjectRBTreeMap() : ((Char2ObjectRBTreeMap) current).clone()));
        register(Char2ObjectOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Char2ObjectOpenCustomHashMap) current).clone()));
        register(Char2ObjectSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ObjectAVLTreeMap();
            var result = new Char2ObjectAVLTreeMap(((Char2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2ObjectSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ObjectAVLTreeMap();
            var result = new Char2ObjectAVLTreeMap(((Char2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ObjectSortedMaps.unmodifiable((Char2ObjectSortedMap) decoded)));
        register(Char2ObjectSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ObjectAVLTreeMap();
            var result = new Char2ObjectAVLTreeMap(((Char2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ObjectSortedMaps.unmodifiable((Char2ObjectSortedMap) decoded)));
        register(Char2ObjectSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ObjectAVLTreeMap();
            var result = new Char2ObjectAVLTreeMap(((Char2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ObjectSortedMaps.unmodifiable((Char2ObjectSortedMap) decoded)));
        register(Char2ObjectSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ObjectAVLTreeMap();
            var result = new Char2ObjectAVLTreeMap(((Char2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ObjectSortedMaps.synchronize((Char2ObjectSortedMap) decoded)));
        register(Char2ObjectMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ObjectMaps.unmodifiable((Char2ObjectMap) decoded)));
        register(Char2ObjectMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ObjectMaps.unmodifiable((Char2ObjectMap) decoded)));
        register(Char2ObjectMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Char2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ObjectMaps.unmodifiable((Char2ObjectMap) decoded)));
        register(Char2ObjectMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ObjectMaps.synchronize((Char2ObjectMap) decoded)));
        register(Char2ReferenceMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ReferenceMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2ReferenceOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Char2ReferenceOpenHashMap() : ((Char2ReferenceOpenHashMap) current).clone()));
        register(Char2ReferenceArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ReferenceArrayMap() : ((Char2ReferenceArrayMap) current).clone()));
        register(Char2ReferenceLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Char2ReferenceLinkedOpenHashMap() : ((Char2ReferenceLinkedOpenHashMap) current).clone()));
        register(Char2ReferenceAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ReferenceAVLTreeMap() : ((Char2ReferenceAVLTreeMap) current).clone()));
        register(Char2ReferenceRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Char2ReferenceRBTreeMap() : ((Char2ReferenceRBTreeMap) current).clone()));
        register(Char2ReferenceOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Char2ReferenceOpenCustomHashMap) current).clone()));
        register(Char2ReferenceSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ReferenceAVLTreeMap();
            var result = new Char2ReferenceAVLTreeMap(((Char2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Char2ReferenceSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ReferenceAVLTreeMap();
            var result = new Char2ReferenceAVLTreeMap(((Char2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ReferenceSortedMaps.unmodifiable((Char2ReferenceSortedMap) decoded)));
        register(Char2ReferenceSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ReferenceAVLTreeMap();
            var result = new Char2ReferenceAVLTreeMap(((Char2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ReferenceSortedMaps.unmodifiable((Char2ReferenceSortedMap) decoded)));
        register(Char2ReferenceSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ReferenceAVLTreeMap();
            var result = new Char2ReferenceAVLTreeMap(((Char2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ReferenceSortedMaps.unmodifiable((Char2ReferenceSortedMap) decoded)));
        register(Char2ReferenceSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Char2ReferenceAVLTreeMap();
            var result = new Char2ReferenceAVLTreeMap(((Char2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Char2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ReferenceSortedMaps.synchronize((Char2ReferenceSortedMap) decoded)));
        register(Char2ReferenceMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ReferenceMaps.unmodifiable((Char2ReferenceMap) decoded)));
        register(Char2ReferenceMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ReferenceMaps.unmodifiable((Char2ReferenceMap) decoded)));
        register(Char2ReferenceMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Char2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ReferenceMaps.unmodifiable((Char2ReferenceMap) decoded)));
        register(Char2ReferenceMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Char2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Char2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Char2ReferenceMaps.synchronize((Char2ReferenceMap) decoded)));
        register(CharCollection.class, (type, current) -> new Construction(() -> new CharArrayList()));
        register(CharList.class, (type, current) -> new Construction(() -> new CharArrayList()));
        register(CharSet.class, (type, current) -> new Construction(() -> new CharOpenHashSet()));
        register(CharSortedSet.class, (type, current) -> new Construction(() -> current == null ? new CharRBTreeSet() :
                new CharRBTreeSet(((CharSortedSet) current).comparator())));
        register(CharBigList.class, (type, current) -> new Construction(() -> new CharBigArrayBigList()));
        register(CharArrayList.class, (type, current) -> new Construction(
                () -> current == null ? new CharArrayList() : ((CharArrayList) current).clone()));
        register(CharArraySet.class, (type, current) -> new Construction(
                () -> current == null ? new CharArraySet() : ((CharArraySet) current).clone()));
        register(CharOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new CharOpenHashSet() : ((CharOpenHashSet) current).clone()));
        register(CharLinkedOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new CharLinkedOpenHashSet() : ((CharLinkedOpenHashSet) current).clone()));
        register(CharAVLTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new CharAVLTreeSet() : ((CharAVLTreeSet) current).clone()));
        register(CharRBTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new CharRBTreeSet() : ((CharRBTreeSet) current).clone()));
        register(CharBigArrayBigList.class, (type, current) -> new Construction(
                () -> current == null ? new CharBigArrayBigList() : ((CharBigArrayBigList) current).clone()));
        register(CharOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((CharOpenCustomHashSet) current).clone()));
        register(CharLinkedOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((CharLinkedOpenCustomHashSet) current).clone()));
        register(CharCollections.unmodifiable(new CharArrayList()).getClass(),
                (type, current) -> new Construction(() -> new CharArrayList(),
                        decoded -> CharCollections.unmodifiable((CharCollection) decoded)));
        register(CharCollections.unmodifiable(new CharArrayList()).getClass(),
                (type, current) -> new Construction(() -> new CharArrayList(),
                        decoded -> CharCollections.unmodifiable((CharCollection) decoded)));
        register(CharCollections.synchronize(new CharArrayList()).getClass(),
                (type, current) -> new Construction(() -> new CharArrayList(),
                        decoded -> CharCollections.synchronize((CharCollection) decoded)));
        register(CharLists.UnmodifiableList.class, (type, current) -> new Construction(() -> new CharArrayList(),
                decoded -> CharLists.unmodifiable((CharList) decoded)));
        register(CharLists.EmptyList.class, (type, current) -> new Construction(() -> new CharArrayList(),
                decoded -> CharLists.unmodifiable((CharList) decoded)));
        register(CharLists.Singleton.class, (type, current) -> new Construction(() -> new CharArrayList(),
                decoded -> CharLists.unmodifiable((CharList) decoded)));
        register(CharLists.SynchronizedList.class, (type, current) -> new Construction(() -> new CharArrayList(),
                decoded -> CharLists.synchronize((CharList) decoded)));
        register(CharLists.UnmodifiableRandomAccessList.class,
                (type, current) -> new Construction(() -> new CharArrayList(),
                        decoded -> CharLists.unmodifiable((CharList) decoded)));
        register(CharLists.SynchronizedRandomAccessList.class,
                (type, current) -> new Construction(() -> new CharArrayList(),
                        decoded -> CharLists.synchronize((CharList) decoded)));
        register(CharSets.UnmodifiableSet.class, (type, current) -> new Construction(() -> new CharOpenHashSet(),
                decoded -> CharSets.unmodifiable((CharSet) decoded)));
        register(CharSets.EmptySet.class, (type, current) -> new Construction(() -> new CharOpenHashSet(),
                decoded -> CharSets.unmodifiable((CharSet) decoded)));
        register(CharSets.Singleton.class, (type, current) -> new Construction(() -> new CharOpenHashSet(),
                decoded -> CharSets.unmodifiable((CharSet) decoded)));
        register(CharSets.SynchronizedSet.class, (type, current) -> new Construction(() -> new CharOpenHashSet(),
                decoded -> CharSets.synchronize((CharSet) decoded)));
        register(CharSortedSets.UnmodifiableSortedSet.class,
                (type, current) -> new Construction(
                        () -> new CharRBTreeSet(current == null ? null : ((CharSortedSet) current).comparator()),
                        decoded -> CharSortedSets.unmodifiable((CharSortedSet) decoded)));
        register(CharSortedSets.EmptySet.class,
                (type, current) -> new Construction(
                        () -> new CharRBTreeSet(current == null ? null : ((CharSortedSet) current).comparator()),
                        decoded -> CharSortedSets.unmodifiable((CharSortedSet) decoded)));
        register(CharSortedSets.Singleton.class,
                (type, current) -> new Construction(
                        () -> new CharRBTreeSet(current == null ? null : ((CharSortedSet) current).comparator()),
                        decoded -> CharSortedSets.unmodifiable((CharSortedSet) decoded)));
        register(CharSortedSets.SynchronizedSortedSet.class,
                (type, current) -> new Construction(
                        () -> new CharRBTreeSet(current == null ? null : ((CharSortedSet) current).comparator()),
                        decoded -> CharSortedSets.synchronize((CharSortedSet) decoded)));
        register(CharBigLists.UnmodifiableBigList.class,
                (type, current) -> new Construction(() -> new CharBigArrayBigList(),
                        decoded -> CharBigLists.unmodifiable((CharBigList) decoded)));
        register(CharBigLists.EmptyBigList.class, (type, current) -> new Construction(() -> new CharBigArrayBigList(),
                decoded -> CharBigLists.unmodifiable((CharBigList) decoded)));
        register(CharBigLists.Singleton.class, (type, current) -> new Construction(() -> new CharBigArrayBigList(),
                decoded -> CharBigLists.unmodifiable((CharBigList) decoded)));
        register(CharBigLists.SynchronizedBigList.class,
                (type, current) -> new Construction(() -> new CharBigArrayBigList(),
                        decoded -> CharBigLists.synchronize((CharBigList) decoded)));
        register(CharImmutableList.class, (type, current) -> new Construction(() -> new CharArrayList(),
                decoded -> new CharImmutableList((Collection) decoded)));
        register(new CharImmutableList(new char[] { 0, 1 }).subList(0, 1).getClass(),
                (type, current) -> new Construction(() -> new CharArrayList(),
                        decoded -> new CharImmutableList((Collection) decoded)));
        register(CharMappedBigList.class, (type, current) -> new Construction(() -> new CharBigArrayBigList(),
                decoded -> updateMapped((BigList) current, (BigList) decoded)));
    }

    private static void registerInt() {
        register(Int2BooleanMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2BooleanMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2BooleanOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2BooleanOpenHashMap() : ((Int2BooleanOpenHashMap) current).clone()));
        register(Int2BooleanArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2BooleanArrayMap() : ((Int2BooleanArrayMap) current).clone()));
        register(Int2BooleanLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Int2BooleanLinkedOpenHashMap() : ((Int2BooleanLinkedOpenHashMap) current).clone()));
        register(Int2BooleanAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2BooleanAVLTreeMap() : ((Int2BooleanAVLTreeMap) current).clone()));
        register(Int2BooleanRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2BooleanRBTreeMap() : ((Int2BooleanRBTreeMap) current).clone()));
        register(Int2BooleanOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Int2BooleanOpenCustomHashMap) current).clone()));
        register(Int2BooleanSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2BooleanAVLTreeMap();
            var result = new Int2BooleanAVLTreeMap(((Int2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Int2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2BooleanSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2BooleanAVLTreeMap();
            var result = new Int2BooleanAVLTreeMap(((Int2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Int2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2BooleanSortedMaps.unmodifiable((Int2BooleanSortedMap) decoded)));
        register(Int2BooleanSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2BooleanAVLTreeMap();
            var result = new Int2BooleanAVLTreeMap(((Int2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Int2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2BooleanSortedMaps.unmodifiable((Int2BooleanSortedMap) decoded)));
        register(Int2BooleanSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2BooleanAVLTreeMap();
            var result = new Int2BooleanAVLTreeMap(((Int2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Int2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2BooleanSortedMaps.unmodifiable((Int2BooleanSortedMap) decoded)));
        register(Int2BooleanSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2BooleanAVLTreeMap();
            var result = new Int2BooleanAVLTreeMap(((Int2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Int2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2BooleanSortedMaps.synchronize((Int2BooleanSortedMap) decoded)));
        register(Int2BooleanMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2BooleanMaps.unmodifiable((Int2BooleanMap) decoded)));
        register(Int2BooleanMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2BooleanMaps.unmodifiable((Int2BooleanMap) decoded)));
        register(Int2BooleanMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Int2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2BooleanMaps.unmodifiable((Int2BooleanMap) decoded)));
        register(Int2BooleanMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2BooleanMaps.synchronize((Int2BooleanMap) decoded)));
        register(Int2ByteMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ByteMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2ByteOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ByteOpenHashMap() : ((Int2ByteOpenHashMap) current).clone()));
        register(Int2ByteArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ByteArrayMap() : ((Int2ByteArrayMap) current).clone()));
        register(Int2ByteLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Int2ByteLinkedOpenHashMap() : ((Int2ByteLinkedOpenHashMap) current).clone()));
        register(Int2ByteAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ByteAVLTreeMap() : ((Int2ByteAVLTreeMap) current).clone()));
        register(Int2ByteRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ByteRBTreeMap() : ((Int2ByteRBTreeMap) current).clone()));
        register(Int2ByteOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Int2ByteOpenCustomHashMap) current).clone()));
        register(Int2ByteSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ByteAVLTreeMap();
            var result = new Int2ByteAVLTreeMap(((Int2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ByteSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2ByteSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ByteAVLTreeMap();
            var result = new Int2ByteAVLTreeMap(((Int2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ByteSortedMaps.unmodifiable((Int2ByteSortedMap) decoded)));
        register(Int2ByteSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ByteAVLTreeMap();
            var result = new Int2ByteAVLTreeMap(((Int2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ByteSortedMaps.unmodifiable((Int2ByteSortedMap) decoded)));
        register(Int2ByteSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ByteAVLTreeMap();
            var result = new Int2ByteAVLTreeMap(((Int2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ByteSortedMaps.unmodifiable((Int2ByteSortedMap) decoded)));
        register(Int2ByteSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ByteAVLTreeMap();
            var result = new Int2ByteAVLTreeMap(((Int2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ByteSortedMaps.synchronize((Int2ByteSortedMap) decoded)));
        register(Int2ByteMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ByteMaps.unmodifiable((Int2ByteMap) decoded)));
        register(Int2ByteMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ByteMaps.unmodifiable((Int2ByteMap) decoded)));
        register(Int2ByteMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Int2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ByteMaps.unmodifiable((Int2ByteMap) decoded)));
        register(Int2ByteMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ByteMaps.synchronize((Int2ByteMap) decoded)));
        register(Int2ShortMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ShortMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2ShortOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ShortOpenHashMap() : ((Int2ShortOpenHashMap) current).clone()));
        register(Int2ShortArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ShortArrayMap() : ((Int2ShortArrayMap) current).clone()));
        register(Int2ShortLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Int2ShortLinkedOpenHashMap() : ((Int2ShortLinkedOpenHashMap) current).clone()));
        register(Int2ShortAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ShortAVLTreeMap() : ((Int2ShortAVLTreeMap) current).clone()));
        register(Int2ShortRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ShortRBTreeMap() : ((Int2ShortRBTreeMap) current).clone()));
        register(Int2ShortOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Int2ShortOpenCustomHashMap) current).clone()));
        register(Int2ShortSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ShortAVLTreeMap();
            var result = new Int2ShortAVLTreeMap(((Int2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ShortSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2ShortSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ShortAVLTreeMap();
            var result = new Int2ShortAVLTreeMap(((Int2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ShortSortedMaps.unmodifiable((Int2ShortSortedMap) decoded)));
        register(Int2ShortSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ShortAVLTreeMap();
            var result = new Int2ShortAVLTreeMap(((Int2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ShortSortedMaps.unmodifiable((Int2ShortSortedMap) decoded)));
        register(Int2ShortSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ShortAVLTreeMap();
            var result = new Int2ShortAVLTreeMap(((Int2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ShortSortedMaps.unmodifiable((Int2ShortSortedMap) decoded)));
        register(Int2ShortSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ShortAVLTreeMap();
            var result = new Int2ShortAVLTreeMap(((Int2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ShortSortedMaps.synchronize((Int2ShortSortedMap) decoded)));
        register(Int2ShortMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ShortMaps.unmodifiable((Int2ShortMap) decoded)));
        register(Int2ShortMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ShortMaps.unmodifiable((Int2ShortMap) decoded)));
        register(Int2ShortMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Int2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ShortMaps.unmodifiable((Int2ShortMap) decoded)));
        register(Int2ShortMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ShortMaps.synchronize((Int2ShortMap) decoded)));
        register(Int2CharMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2CharMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2CharOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2CharOpenHashMap() : ((Int2CharOpenHashMap) current).clone()));
        register(Int2CharArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2CharArrayMap() : ((Int2CharArrayMap) current).clone()));
        register(Int2CharLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Int2CharLinkedOpenHashMap() : ((Int2CharLinkedOpenHashMap) current).clone()));
        register(Int2CharAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2CharAVLTreeMap() : ((Int2CharAVLTreeMap) current).clone()));
        register(Int2CharRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2CharRBTreeMap() : ((Int2CharRBTreeMap) current).clone()));
        register(Int2CharOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Int2CharOpenCustomHashMap) current).clone()));
        register(Int2CharSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2CharAVLTreeMap();
            var result = new Int2CharAVLTreeMap(((Int2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Int2CharSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2CharSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2CharAVLTreeMap();
            var result = new Int2CharAVLTreeMap(((Int2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Int2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2CharSortedMaps.unmodifiable((Int2CharSortedMap) decoded)));
        register(Int2CharSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2CharAVLTreeMap();
            var result = new Int2CharAVLTreeMap(((Int2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Int2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2CharSortedMaps.unmodifiable((Int2CharSortedMap) decoded)));
        register(Int2CharSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2CharAVLTreeMap();
            var result = new Int2CharAVLTreeMap(((Int2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Int2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2CharSortedMaps.unmodifiable((Int2CharSortedMap) decoded)));
        register(Int2CharSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2CharAVLTreeMap();
            var result = new Int2CharAVLTreeMap(((Int2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Int2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2CharSortedMaps.synchronize((Int2CharSortedMap) decoded)));
        register(Int2CharMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2CharMaps.unmodifiable((Int2CharMap) decoded)));
        register(Int2CharMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2CharMaps.unmodifiable((Int2CharMap) decoded)));
        register(Int2CharMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Int2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2CharMaps.unmodifiable((Int2CharMap) decoded)));
        register(Int2CharMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2CharMaps.synchronize((Int2CharMap) decoded)));
        register(Int2IntMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2IntMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2IntOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2IntOpenHashMap() : ((Int2IntOpenHashMap) current).clone()));
        register(Int2IntArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2IntArrayMap() : ((Int2IntArrayMap) current).clone()));
        register(Int2IntLinkedOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2IntLinkedOpenHashMap() : ((Int2IntLinkedOpenHashMap) current).clone()));
        register(Int2IntAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2IntAVLTreeMap() : ((Int2IntAVLTreeMap) current).clone()));
        register(Int2IntRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2IntRBTreeMap() : ((Int2IntRBTreeMap) current).clone()));
        register(Int2IntOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Int2IntOpenCustomHashMap) current).clone()));
        register(Int2IntSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2IntAVLTreeMap();
            var result = new Int2IntAVLTreeMap(((Int2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Int2IntSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2IntSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2IntAVLTreeMap();
            var result = new Int2IntAVLTreeMap(((Int2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Int2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2IntSortedMaps.unmodifiable((Int2IntSortedMap) decoded)));
        register(Int2IntSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2IntAVLTreeMap();
            var result = new Int2IntAVLTreeMap(((Int2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Int2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2IntSortedMaps.unmodifiable((Int2IntSortedMap) decoded)));
        register(Int2IntSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2IntAVLTreeMap();
            var result = new Int2IntAVLTreeMap(((Int2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Int2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2IntSortedMaps.unmodifiable((Int2IntSortedMap) decoded)));
        register(Int2IntSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2IntAVLTreeMap();
            var result = new Int2IntAVLTreeMap(((Int2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Int2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2IntSortedMaps.synchronize((Int2IntSortedMap) decoded)));
        register(Int2IntMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2IntMaps.unmodifiable((Int2IntMap) decoded)));
        register(Int2IntMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2IntMaps.unmodifiable((Int2IntMap) decoded)));
        register(Int2IntMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Int2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2IntMaps.unmodifiable((Int2IntMap) decoded)));
        register(Int2IntMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2IntMaps.synchronize((Int2IntMap) decoded)));
        register(Int2LongMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2LongMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2LongOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2LongOpenHashMap() : ((Int2LongOpenHashMap) current).clone()));
        register(Int2LongArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2LongArrayMap() : ((Int2LongArrayMap) current).clone()));
        register(Int2LongLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Int2LongLinkedOpenHashMap() : ((Int2LongLinkedOpenHashMap) current).clone()));
        register(Int2LongAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2LongAVLTreeMap() : ((Int2LongAVLTreeMap) current).clone()));
        register(Int2LongRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2LongRBTreeMap() : ((Int2LongRBTreeMap) current).clone()));
        register(Int2LongOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Int2LongOpenCustomHashMap) current).clone()));
        register(Int2LongSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2LongAVLTreeMap();
            var result = new Int2LongAVLTreeMap(((Int2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Int2LongSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2LongSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2LongAVLTreeMap();
            var result = new Int2LongAVLTreeMap(((Int2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Int2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2LongSortedMaps.unmodifiable((Int2LongSortedMap) decoded)));
        register(Int2LongSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2LongAVLTreeMap();
            var result = new Int2LongAVLTreeMap(((Int2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Int2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2LongSortedMaps.unmodifiable((Int2LongSortedMap) decoded)));
        register(Int2LongSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2LongAVLTreeMap();
            var result = new Int2LongAVLTreeMap(((Int2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Int2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2LongSortedMaps.unmodifiable((Int2LongSortedMap) decoded)));
        register(Int2LongSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2LongAVLTreeMap();
            var result = new Int2LongAVLTreeMap(((Int2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Int2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2LongSortedMaps.synchronize((Int2LongSortedMap) decoded)));
        register(Int2LongMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2LongMaps.unmodifiable((Int2LongMap) decoded)));
        register(Int2LongMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2LongMaps.unmodifiable((Int2LongMap) decoded)));
        register(Int2LongMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Int2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2LongMaps.unmodifiable((Int2LongMap) decoded)));
        register(Int2LongMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2LongMaps.synchronize((Int2LongMap) decoded)));
        register(Int2FloatMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2FloatMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2FloatOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2FloatOpenHashMap() : ((Int2FloatOpenHashMap) current).clone()));
        register(Int2FloatArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2FloatArrayMap() : ((Int2FloatArrayMap) current).clone()));
        register(Int2FloatLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Int2FloatLinkedOpenHashMap() : ((Int2FloatLinkedOpenHashMap) current).clone()));
        register(Int2FloatAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2FloatAVLTreeMap() : ((Int2FloatAVLTreeMap) current).clone()));
        register(Int2FloatRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2FloatRBTreeMap() : ((Int2FloatRBTreeMap) current).clone()));
        register(Int2FloatOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Int2FloatOpenCustomHashMap) current).clone()));
        register(Int2FloatSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2FloatAVLTreeMap();
            var result = new Int2FloatAVLTreeMap(((Int2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Int2FloatSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2FloatSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2FloatAVLTreeMap();
            var result = new Int2FloatAVLTreeMap(((Int2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Int2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2FloatSortedMaps.unmodifiable((Int2FloatSortedMap) decoded)));
        register(Int2FloatSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2FloatAVLTreeMap();
            var result = new Int2FloatAVLTreeMap(((Int2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Int2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2FloatSortedMaps.unmodifiable((Int2FloatSortedMap) decoded)));
        register(Int2FloatSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2FloatAVLTreeMap();
            var result = new Int2FloatAVLTreeMap(((Int2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Int2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2FloatSortedMaps.unmodifiable((Int2FloatSortedMap) decoded)));
        register(Int2FloatSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2FloatAVLTreeMap();
            var result = new Int2FloatAVLTreeMap(((Int2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Int2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2FloatSortedMaps.synchronize((Int2FloatSortedMap) decoded)));
        register(Int2FloatMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2FloatMaps.unmodifiable((Int2FloatMap) decoded)));
        register(Int2FloatMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2FloatMaps.unmodifiable((Int2FloatMap) decoded)));
        register(Int2FloatMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Int2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2FloatMaps.unmodifiable((Int2FloatMap) decoded)));
        register(Int2FloatMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2FloatMaps.synchronize((Int2FloatMap) decoded)));
        register(Int2DoubleMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2DoubleMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2DoubleOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2DoubleOpenHashMap() : ((Int2DoubleOpenHashMap) current).clone()));
        register(Int2DoubleArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2DoubleArrayMap() : ((Int2DoubleArrayMap) current).clone()));
        register(Int2DoubleLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Int2DoubleLinkedOpenHashMap() : ((Int2DoubleLinkedOpenHashMap) current).clone()));
        register(Int2DoubleAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2DoubleAVLTreeMap() : ((Int2DoubleAVLTreeMap) current).clone()));
        register(Int2DoubleRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2DoubleRBTreeMap() : ((Int2DoubleRBTreeMap) current).clone()));
        register(Int2DoubleOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Int2DoubleOpenCustomHashMap) current).clone()));
        register(Int2DoubleSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2DoubleAVLTreeMap();
            var result = new Int2DoubleAVLTreeMap(((Int2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Int2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2DoubleSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2DoubleAVLTreeMap();
            var result = new Int2DoubleAVLTreeMap(((Int2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Int2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2DoubleSortedMaps.unmodifiable((Int2DoubleSortedMap) decoded)));
        register(Int2DoubleSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2DoubleAVLTreeMap();
            var result = new Int2DoubleAVLTreeMap(((Int2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Int2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2DoubleSortedMaps.unmodifiable((Int2DoubleSortedMap) decoded)));
        register(Int2DoubleSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2DoubleAVLTreeMap();
            var result = new Int2DoubleAVLTreeMap(((Int2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Int2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2DoubleSortedMaps.unmodifiable((Int2DoubleSortedMap) decoded)));
        register(Int2DoubleSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2DoubleAVLTreeMap();
            var result = new Int2DoubleAVLTreeMap(((Int2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Int2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2DoubleSortedMaps.synchronize((Int2DoubleSortedMap) decoded)));
        register(Int2DoubleMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2DoubleMaps.unmodifiable((Int2DoubleMap) decoded)));
        register(Int2DoubleMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2DoubleMaps.unmodifiable((Int2DoubleMap) decoded)));
        register(Int2DoubleMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Int2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2DoubleMaps.unmodifiable((Int2DoubleMap) decoded)));
        register(Int2DoubleMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2DoubleMaps.synchronize((Int2DoubleMap) decoded)));
        register(Int2ObjectMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ObjectMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2ObjectOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ObjectOpenHashMap() : ((Int2ObjectOpenHashMap) current).clone()));
        register(Int2ObjectArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ObjectArrayMap() : ((Int2ObjectArrayMap) current).clone()));
        register(Int2ObjectLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Int2ObjectLinkedOpenHashMap() : ((Int2ObjectLinkedOpenHashMap) current).clone()));
        register(Int2ObjectAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ObjectAVLTreeMap() : ((Int2ObjectAVLTreeMap) current).clone()));
        register(Int2ObjectRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ObjectRBTreeMap() : ((Int2ObjectRBTreeMap) current).clone()));
        register(Int2ObjectOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Int2ObjectOpenCustomHashMap) current).clone()));
        register(Int2ObjectSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ObjectAVLTreeMap();
            var result = new Int2ObjectAVLTreeMap(((Int2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2ObjectSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ObjectAVLTreeMap();
            var result = new Int2ObjectAVLTreeMap(((Int2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ObjectSortedMaps.unmodifiable((Int2ObjectSortedMap) decoded)));
        register(Int2ObjectSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ObjectAVLTreeMap();
            var result = new Int2ObjectAVLTreeMap(((Int2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ObjectSortedMaps.unmodifiable((Int2ObjectSortedMap) decoded)));
        register(Int2ObjectSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ObjectAVLTreeMap();
            var result = new Int2ObjectAVLTreeMap(((Int2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ObjectSortedMaps.unmodifiable((Int2ObjectSortedMap) decoded)));
        register(Int2ObjectSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ObjectAVLTreeMap();
            var result = new Int2ObjectAVLTreeMap(((Int2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ObjectSortedMaps.synchronize((Int2ObjectSortedMap) decoded)));
        register(Int2ObjectMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ObjectMaps.unmodifiable((Int2ObjectMap) decoded)));
        register(Int2ObjectMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ObjectMaps.unmodifiable((Int2ObjectMap) decoded)));
        register(Int2ObjectMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Int2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ObjectMaps.unmodifiable((Int2ObjectMap) decoded)));
        register(Int2ObjectMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ObjectMaps.synchronize((Int2ObjectMap) decoded)));
        register(Int2ReferenceMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ReferenceMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2ReferenceOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ReferenceOpenHashMap() : ((Int2ReferenceOpenHashMap) current).clone()));
        register(Int2ReferenceArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ReferenceArrayMap() : ((Int2ReferenceArrayMap) current).clone()));
        register(Int2ReferenceLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Int2ReferenceLinkedOpenHashMap() : ((Int2ReferenceLinkedOpenHashMap) current).clone()));
        register(Int2ReferenceAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ReferenceAVLTreeMap() : ((Int2ReferenceAVLTreeMap) current).clone()));
        register(Int2ReferenceRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Int2ReferenceRBTreeMap() : ((Int2ReferenceRBTreeMap) current).clone()));
        register(Int2ReferenceOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Int2ReferenceOpenCustomHashMap) current).clone()));
        register(Int2ReferenceSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ReferenceAVLTreeMap();
            var result = new Int2ReferenceAVLTreeMap(((Int2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Int2ReferenceSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ReferenceAVLTreeMap();
            var result = new Int2ReferenceAVLTreeMap(((Int2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ReferenceSortedMaps.unmodifiable((Int2ReferenceSortedMap) decoded)));
        register(Int2ReferenceSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ReferenceAVLTreeMap();
            var result = new Int2ReferenceAVLTreeMap(((Int2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ReferenceSortedMaps.unmodifiable((Int2ReferenceSortedMap) decoded)));
        register(Int2ReferenceSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ReferenceAVLTreeMap();
            var result = new Int2ReferenceAVLTreeMap(((Int2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ReferenceSortedMaps.unmodifiable((Int2ReferenceSortedMap) decoded)));
        register(Int2ReferenceSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Int2ReferenceAVLTreeMap();
            var result = new Int2ReferenceAVLTreeMap(((Int2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Int2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ReferenceSortedMaps.synchronize((Int2ReferenceSortedMap) decoded)));
        register(Int2ReferenceMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ReferenceMaps.unmodifiable((Int2ReferenceMap) decoded)));
        register(Int2ReferenceMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ReferenceMaps.unmodifiable((Int2ReferenceMap) decoded)));
        register(Int2ReferenceMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Int2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ReferenceMaps.unmodifiable((Int2ReferenceMap) decoded)));
        register(Int2ReferenceMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Int2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Int2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Int2ReferenceMaps.synchronize((Int2ReferenceMap) decoded)));
        register(IntCollection.class, (type, current) -> new Construction(() -> new IntArrayList()));
        register(IntList.class, (type, current) -> new Construction(() -> new IntArrayList()));
        register(IntSet.class, (type, current) -> new Construction(() -> new IntOpenHashSet()));
        register(IntSortedSet.class, (type, current) -> new Construction(
                () -> current == null ? new IntRBTreeSet() : new IntRBTreeSet(((IntSortedSet) current).comparator())));
        register(IntBigList.class, (type, current) -> new Construction(() -> new IntBigArrayBigList()));
        register(IntArrayList.class, (type, current) -> new Construction(
                () -> current == null ? new IntArrayList() : ((IntArrayList) current).clone()));
        register(IntArraySet.class, (type, current) -> new Construction(
                () -> current == null ? new IntArraySet() : ((IntArraySet) current).clone()));
        register(IntOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new IntOpenHashSet() : ((IntOpenHashSet) current).clone()));
        register(IntLinkedOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new IntLinkedOpenHashSet() : ((IntLinkedOpenHashSet) current).clone()));
        register(IntAVLTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new IntAVLTreeSet() : ((IntAVLTreeSet) current).clone()));
        register(IntRBTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new IntRBTreeSet() : ((IntRBTreeSet) current).clone()));
        register(IntBigArrayBigList.class, (type, current) -> new Construction(
                () -> current == null ? new IntBigArrayBigList() : ((IntBigArrayBigList) current).clone()));
        register(IntOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((IntOpenCustomHashSet) current).clone()));
        register(IntLinkedOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((IntLinkedOpenCustomHashSet) current).clone()));
        register(IntCollections.unmodifiable(new IntArrayList()).getClass(),
                (type, current) -> new Construction(() -> new IntArrayList(),
                        decoded -> IntCollections.unmodifiable((IntCollection) decoded)));
        register(IntCollections.unmodifiable(new IntArrayList()).getClass(),
                (type, current) -> new Construction(() -> new IntArrayList(),
                        decoded -> IntCollections.unmodifiable((IntCollection) decoded)));
        register(IntCollections.synchronize(new IntArrayList()).getClass(),
                (type, current) -> new Construction(() -> new IntArrayList(),
                        decoded -> IntCollections.synchronize((IntCollection) decoded)));
        register(IntLists.UnmodifiableList.class, (type, current) -> new Construction(() -> new IntArrayList(),
                decoded -> IntLists.unmodifiable((IntList) decoded)));
        register(IntLists.EmptyList.class, (type, current) -> new Construction(() -> new IntArrayList(),
                decoded -> IntLists.unmodifiable((IntList) decoded)));
        register(IntLists.Singleton.class, (type, current) -> new Construction(() -> new IntArrayList(),
                decoded -> IntLists.unmodifiable((IntList) decoded)));
        register(IntLists.SynchronizedList.class, (type, current) -> new Construction(() -> new IntArrayList(),
                decoded -> IntLists.synchronize((IntList) decoded)));
        register(IntLists.UnmodifiableRandomAccessList.class,
                (type, current) -> new Construction(() -> new IntArrayList(),
                        decoded -> IntLists.unmodifiable((IntList) decoded)));
        register(IntLists.SynchronizedRandomAccessList.class,
                (type, current) -> new Construction(() -> new IntArrayList(),
                        decoded -> IntLists.synchronize((IntList) decoded)));
        register(IntSets.UnmodifiableSet.class, (type, current) -> new Construction(() -> new IntOpenHashSet(),
                decoded -> IntSets.unmodifiable((IntSet) decoded)));
        register(IntSets.EmptySet.class, (type, current) -> new Construction(() -> new IntOpenHashSet(),
                decoded -> IntSets.unmodifiable((IntSet) decoded)));
        register(IntSets.Singleton.class, (type, current) -> new Construction(() -> new IntOpenHashSet(),
                decoded -> IntSets.unmodifiable((IntSet) decoded)));
        register(IntSets.SynchronizedSet.class, (type, current) -> new Construction(() -> new IntOpenHashSet(),
                decoded -> IntSets.synchronize((IntSet) decoded)));
        register(IntSortedSets.UnmodifiableSortedSet.class,
                (type, current) -> new Construction(
                        () -> new IntRBTreeSet(current == null ? null : ((IntSortedSet) current).comparator()),
                        decoded -> IntSortedSets.unmodifiable((IntSortedSet) decoded)));
        register(IntSortedSets.EmptySet.class,
                (type, current) -> new Construction(
                        () -> new IntRBTreeSet(current == null ? null : ((IntSortedSet) current).comparator()),
                        decoded -> IntSortedSets.unmodifiable((IntSortedSet) decoded)));
        register(IntSortedSets.Singleton.class,
                (type, current) -> new Construction(
                        () -> new IntRBTreeSet(current == null ? null : ((IntSortedSet) current).comparator()),
                        decoded -> IntSortedSets.unmodifiable((IntSortedSet) decoded)));
        register(IntSortedSets.SynchronizedSortedSet.class,
                (type, current) -> new Construction(
                        () -> new IntRBTreeSet(current == null ? null : ((IntSortedSet) current).comparator()),
                        decoded -> IntSortedSets.synchronize((IntSortedSet) decoded)));
        register(IntBigLists.UnmodifiableBigList.class,
                (type, current) -> new Construction(() -> new IntBigArrayBigList(),
                        decoded -> IntBigLists.unmodifiable((IntBigList) decoded)));
        register(IntBigLists.EmptyBigList.class, (type, current) -> new Construction(() -> new IntBigArrayBigList(),
                decoded -> IntBigLists.unmodifiable((IntBigList) decoded)));
        register(IntBigLists.Singleton.class, (type, current) -> new Construction(() -> new IntBigArrayBigList(),
                decoded -> IntBigLists.unmodifiable((IntBigList) decoded)));
        register(IntBigLists.SynchronizedBigList.class,
                (type, current) -> new Construction(() -> new IntBigArrayBigList(),
                        decoded -> IntBigLists.synchronize((IntBigList) decoded)));
        register(IntImmutableList.class, (type, current) -> new Construction(() -> new IntArrayList(),
                decoded -> new IntImmutableList((Collection) decoded)));
        register(new IntImmutableList(new int[] { 0, 1 }).subList(0, 1).getClass(),
                (type, current) -> new Construction(() -> new IntArrayList(),
                        decoded -> new IntImmutableList((Collection) decoded)));
        register(IntMappedBigList.class, (type, current) -> new Construction(() -> new IntBigArrayBigList(),
                decoded -> updateMapped((BigList) current, (BigList) decoded)));
    }

    private static void registerLong() {
        register(Long2BooleanMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2BooleanMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2BooleanOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2BooleanOpenHashMap() : ((Long2BooleanOpenHashMap) current).clone()));
        register(Long2BooleanArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2BooleanArrayMap() : ((Long2BooleanArrayMap) current).clone()));
        register(Long2BooleanLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Long2BooleanLinkedOpenHashMap() : ((Long2BooleanLinkedOpenHashMap) current).clone()));
        register(Long2BooleanAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2BooleanAVLTreeMap() : ((Long2BooleanAVLTreeMap) current).clone()));
        register(Long2BooleanRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2BooleanRBTreeMap() : ((Long2BooleanRBTreeMap) current).clone()));
        register(Long2BooleanOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Long2BooleanOpenCustomHashMap) current).clone()));
        register(Long2BooleanSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2BooleanAVLTreeMap();
            var result = new Long2BooleanAVLTreeMap(((Long2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Long2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2BooleanSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2BooleanAVLTreeMap();
            var result = new Long2BooleanAVLTreeMap(((Long2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Long2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2BooleanSortedMaps.unmodifiable((Long2BooleanSortedMap) decoded)));
        register(Long2BooleanSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2BooleanAVLTreeMap();
            var result = new Long2BooleanAVLTreeMap(((Long2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Long2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2BooleanSortedMaps.unmodifiable((Long2BooleanSortedMap) decoded)));
        register(Long2BooleanSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2BooleanAVLTreeMap();
            var result = new Long2BooleanAVLTreeMap(((Long2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Long2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2BooleanSortedMaps.unmodifiable((Long2BooleanSortedMap) decoded)));
        register(Long2BooleanSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2BooleanAVLTreeMap();
            var result = new Long2BooleanAVLTreeMap(((Long2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Long2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2BooleanSortedMaps.synchronize((Long2BooleanSortedMap) decoded)));
        register(Long2BooleanMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2BooleanMaps.unmodifiable((Long2BooleanMap) decoded)));
        register(Long2BooleanMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2BooleanMaps.unmodifiable((Long2BooleanMap) decoded)));
        register(Long2BooleanMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Long2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2BooleanMaps.unmodifiable((Long2BooleanMap) decoded)));
        register(Long2BooleanMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2BooleanMaps.synchronize((Long2BooleanMap) decoded)));
        register(Long2ByteMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ByteMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2ByteOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ByteOpenHashMap() : ((Long2ByteOpenHashMap) current).clone()));
        register(Long2ByteArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ByteArrayMap() : ((Long2ByteArrayMap) current).clone()));
        register(Long2ByteLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Long2ByteLinkedOpenHashMap() : ((Long2ByteLinkedOpenHashMap) current).clone()));
        register(Long2ByteAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ByteAVLTreeMap() : ((Long2ByteAVLTreeMap) current).clone()));
        register(Long2ByteRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ByteRBTreeMap() : ((Long2ByteRBTreeMap) current).clone()));
        register(Long2ByteOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Long2ByteOpenCustomHashMap) current).clone()));
        register(Long2ByteSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ByteAVLTreeMap();
            var result = new Long2ByteAVLTreeMap(((Long2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ByteSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2ByteSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ByteAVLTreeMap();
            var result = new Long2ByteAVLTreeMap(((Long2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ByteSortedMaps.unmodifiable((Long2ByteSortedMap) decoded)));
        register(Long2ByteSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ByteAVLTreeMap();
            var result = new Long2ByteAVLTreeMap(((Long2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ByteSortedMaps.unmodifiable((Long2ByteSortedMap) decoded)));
        register(Long2ByteSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ByteAVLTreeMap();
            var result = new Long2ByteAVLTreeMap(((Long2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ByteSortedMaps.unmodifiable((Long2ByteSortedMap) decoded)));
        register(Long2ByteSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ByteAVLTreeMap();
            var result = new Long2ByteAVLTreeMap(((Long2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ByteSortedMaps.synchronize((Long2ByteSortedMap) decoded)));
        register(Long2ByteMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ByteMaps.unmodifiable((Long2ByteMap) decoded)));
        register(Long2ByteMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ByteMaps.unmodifiable((Long2ByteMap) decoded)));
        register(Long2ByteMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Long2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ByteMaps.unmodifiable((Long2ByteMap) decoded)));
        register(Long2ByteMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ByteMaps.synchronize((Long2ByteMap) decoded)));
        register(Long2ShortMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ShortMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2ShortOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ShortOpenHashMap() : ((Long2ShortOpenHashMap) current).clone()));
        register(Long2ShortArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ShortArrayMap() : ((Long2ShortArrayMap) current).clone()));
        register(Long2ShortLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Long2ShortLinkedOpenHashMap() : ((Long2ShortLinkedOpenHashMap) current).clone()));
        register(Long2ShortAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ShortAVLTreeMap() : ((Long2ShortAVLTreeMap) current).clone()));
        register(Long2ShortRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ShortRBTreeMap() : ((Long2ShortRBTreeMap) current).clone()));
        register(Long2ShortOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Long2ShortOpenCustomHashMap) current).clone()));
        register(Long2ShortSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ShortAVLTreeMap();
            var result = new Long2ShortAVLTreeMap(((Long2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ShortSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2ShortSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ShortAVLTreeMap();
            var result = new Long2ShortAVLTreeMap(((Long2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ShortSortedMaps.unmodifiable((Long2ShortSortedMap) decoded)));
        register(Long2ShortSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ShortAVLTreeMap();
            var result = new Long2ShortAVLTreeMap(((Long2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ShortSortedMaps.unmodifiable((Long2ShortSortedMap) decoded)));
        register(Long2ShortSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ShortAVLTreeMap();
            var result = new Long2ShortAVLTreeMap(((Long2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ShortSortedMaps.unmodifiable((Long2ShortSortedMap) decoded)));
        register(Long2ShortSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ShortAVLTreeMap();
            var result = new Long2ShortAVLTreeMap(((Long2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ShortSortedMaps.synchronize((Long2ShortSortedMap) decoded)));
        register(Long2ShortMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ShortMaps.unmodifiable((Long2ShortMap) decoded)));
        register(Long2ShortMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ShortMaps.unmodifiable((Long2ShortMap) decoded)));
        register(Long2ShortMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Long2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ShortMaps.unmodifiable((Long2ShortMap) decoded)));
        register(Long2ShortMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ShortMaps.synchronize((Long2ShortMap) decoded)));
        register(Long2CharMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2CharMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2CharOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2CharOpenHashMap() : ((Long2CharOpenHashMap) current).clone()));
        register(Long2CharArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2CharArrayMap() : ((Long2CharArrayMap) current).clone()));
        register(Long2CharLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Long2CharLinkedOpenHashMap() : ((Long2CharLinkedOpenHashMap) current).clone()));
        register(Long2CharAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2CharAVLTreeMap() : ((Long2CharAVLTreeMap) current).clone()));
        register(Long2CharRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2CharRBTreeMap() : ((Long2CharRBTreeMap) current).clone()));
        register(Long2CharOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Long2CharOpenCustomHashMap) current).clone()));
        register(Long2CharSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2CharAVLTreeMap();
            var result = new Long2CharAVLTreeMap(((Long2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Long2CharSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2CharSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2CharAVLTreeMap();
            var result = new Long2CharAVLTreeMap(((Long2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Long2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2CharSortedMaps.unmodifiable((Long2CharSortedMap) decoded)));
        register(Long2CharSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2CharAVLTreeMap();
            var result = new Long2CharAVLTreeMap(((Long2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Long2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2CharSortedMaps.unmodifiable((Long2CharSortedMap) decoded)));
        register(Long2CharSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2CharAVLTreeMap();
            var result = new Long2CharAVLTreeMap(((Long2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Long2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2CharSortedMaps.unmodifiable((Long2CharSortedMap) decoded)));
        register(Long2CharSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2CharAVLTreeMap();
            var result = new Long2CharAVLTreeMap(((Long2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Long2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2CharSortedMaps.synchronize((Long2CharSortedMap) decoded)));
        register(Long2CharMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2CharMaps.unmodifiable((Long2CharMap) decoded)));
        register(Long2CharMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2CharMaps.unmodifiable((Long2CharMap) decoded)));
        register(Long2CharMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Long2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2CharMaps.unmodifiable((Long2CharMap) decoded)));
        register(Long2CharMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2CharMaps.synchronize((Long2CharMap) decoded)));
        register(Long2IntMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2IntMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2IntOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2IntOpenHashMap() : ((Long2IntOpenHashMap) current).clone()));
        register(Long2IntArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2IntArrayMap() : ((Long2IntArrayMap) current).clone()));
        register(Long2IntLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Long2IntLinkedOpenHashMap() : ((Long2IntLinkedOpenHashMap) current).clone()));
        register(Long2IntAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2IntAVLTreeMap() : ((Long2IntAVLTreeMap) current).clone()));
        register(Long2IntRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2IntRBTreeMap() : ((Long2IntRBTreeMap) current).clone()));
        register(Long2IntOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Long2IntOpenCustomHashMap) current).clone()));
        register(Long2IntSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2IntAVLTreeMap();
            var result = new Long2IntAVLTreeMap(((Long2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Long2IntSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2IntSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2IntAVLTreeMap();
            var result = new Long2IntAVLTreeMap(((Long2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Long2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2IntSortedMaps.unmodifiable((Long2IntSortedMap) decoded)));
        register(Long2IntSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2IntAVLTreeMap();
            var result = new Long2IntAVLTreeMap(((Long2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Long2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2IntSortedMaps.unmodifiable((Long2IntSortedMap) decoded)));
        register(Long2IntSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2IntAVLTreeMap();
            var result = new Long2IntAVLTreeMap(((Long2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Long2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2IntSortedMaps.unmodifiable((Long2IntSortedMap) decoded)));
        register(Long2IntSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2IntAVLTreeMap();
            var result = new Long2IntAVLTreeMap(((Long2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Long2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2IntSortedMaps.synchronize((Long2IntSortedMap) decoded)));
        register(Long2IntMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2IntMaps.unmodifiable((Long2IntMap) decoded)));
        register(Long2IntMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2IntMaps.unmodifiable((Long2IntMap) decoded)));
        register(Long2IntMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Long2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2IntMaps.unmodifiable((Long2IntMap) decoded)));
        register(Long2IntMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2IntMaps.synchronize((Long2IntMap) decoded)));
        register(Long2LongMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2LongMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2LongOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2LongOpenHashMap() : ((Long2LongOpenHashMap) current).clone()));
        register(Long2LongArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2LongArrayMap() : ((Long2LongArrayMap) current).clone()));
        register(Long2LongLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Long2LongLinkedOpenHashMap() : ((Long2LongLinkedOpenHashMap) current).clone()));
        register(Long2LongAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2LongAVLTreeMap() : ((Long2LongAVLTreeMap) current).clone()));
        register(Long2LongRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2LongRBTreeMap() : ((Long2LongRBTreeMap) current).clone()));
        register(Long2LongOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Long2LongOpenCustomHashMap) current).clone()));
        register(Long2LongSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2LongAVLTreeMap();
            var result = new Long2LongAVLTreeMap(((Long2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Long2LongSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2LongSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2LongAVLTreeMap();
            var result = new Long2LongAVLTreeMap(((Long2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Long2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2LongSortedMaps.unmodifiable((Long2LongSortedMap) decoded)));
        register(Long2LongSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2LongAVLTreeMap();
            var result = new Long2LongAVLTreeMap(((Long2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Long2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2LongSortedMaps.unmodifiable((Long2LongSortedMap) decoded)));
        register(Long2LongSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2LongAVLTreeMap();
            var result = new Long2LongAVLTreeMap(((Long2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Long2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2LongSortedMaps.unmodifiable((Long2LongSortedMap) decoded)));
        register(Long2LongSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2LongAVLTreeMap();
            var result = new Long2LongAVLTreeMap(((Long2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Long2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2LongSortedMaps.synchronize((Long2LongSortedMap) decoded)));
        register(Long2LongMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2LongMaps.unmodifiable((Long2LongMap) decoded)));
        register(Long2LongMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2LongMaps.unmodifiable((Long2LongMap) decoded)));
        register(Long2LongMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Long2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2LongMaps.unmodifiable((Long2LongMap) decoded)));
        register(Long2LongMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2LongMaps.synchronize((Long2LongMap) decoded)));
        register(Long2FloatMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2FloatMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2FloatOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2FloatOpenHashMap() : ((Long2FloatOpenHashMap) current).clone()));
        register(Long2FloatArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2FloatArrayMap() : ((Long2FloatArrayMap) current).clone()));
        register(Long2FloatLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Long2FloatLinkedOpenHashMap() : ((Long2FloatLinkedOpenHashMap) current).clone()));
        register(Long2FloatAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2FloatAVLTreeMap() : ((Long2FloatAVLTreeMap) current).clone()));
        register(Long2FloatRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2FloatRBTreeMap() : ((Long2FloatRBTreeMap) current).clone()));
        register(Long2FloatOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Long2FloatOpenCustomHashMap) current).clone()));
        register(Long2FloatSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2FloatAVLTreeMap();
            var result = new Long2FloatAVLTreeMap(((Long2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Long2FloatSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2FloatSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2FloatAVLTreeMap();
            var result = new Long2FloatAVLTreeMap(((Long2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Long2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2FloatSortedMaps.unmodifiable((Long2FloatSortedMap) decoded)));
        register(Long2FloatSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2FloatAVLTreeMap();
            var result = new Long2FloatAVLTreeMap(((Long2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Long2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2FloatSortedMaps.unmodifiable((Long2FloatSortedMap) decoded)));
        register(Long2FloatSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2FloatAVLTreeMap();
            var result = new Long2FloatAVLTreeMap(((Long2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Long2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2FloatSortedMaps.unmodifiable((Long2FloatSortedMap) decoded)));
        register(Long2FloatSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2FloatAVLTreeMap();
            var result = new Long2FloatAVLTreeMap(((Long2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Long2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2FloatSortedMaps.synchronize((Long2FloatSortedMap) decoded)));
        register(Long2FloatMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2FloatMaps.unmodifiable((Long2FloatMap) decoded)));
        register(Long2FloatMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2FloatMaps.unmodifiable((Long2FloatMap) decoded)));
        register(Long2FloatMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Long2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2FloatMaps.unmodifiable((Long2FloatMap) decoded)));
        register(Long2FloatMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2FloatMaps.synchronize((Long2FloatMap) decoded)));
        register(Long2DoubleMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2DoubleMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2DoubleOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2DoubleOpenHashMap() : ((Long2DoubleOpenHashMap) current).clone()));
        register(Long2DoubleArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2DoubleArrayMap() : ((Long2DoubleArrayMap) current).clone()));
        register(Long2DoubleLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Long2DoubleLinkedOpenHashMap() : ((Long2DoubleLinkedOpenHashMap) current).clone()));
        register(Long2DoubleAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2DoubleAVLTreeMap() : ((Long2DoubleAVLTreeMap) current).clone()));
        register(Long2DoubleRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2DoubleRBTreeMap() : ((Long2DoubleRBTreeMap) current).clone()));
        register(Long2DoubleOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Long2DoubleOpenCustomHashMap) current).clone()));
        register(Long2DoubleSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2DoubleAVLTreeMap();
            var result = new Long2DoubleAVLTreeMap(((Long2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Long2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2DoubleSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2DoubleAVLTreeMap();
            var result = new Long2DoubleAVLTreeMap(((Long2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Long2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2DoubleSortedMaps.unmodifiable((Long2DoubleSortedMap) decoded)));
        register(Long2DoubleSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2DoubleAVLTreeMap();
            var result = new Long2DoubleAVLTreeMap(((Long2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Long2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2DoubleSortedMaps.unmodifiable((Long2DoubleSortedMap) decoded)));
        register(Long2DoubleSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2DoubleAVLTreeMap();
            var result = new Long2DoubleAVLTreeMap(((Long2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Long2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2DoubleSortedMaps.unmodifiable((Long2DoubleSortedMap) decoded)));
        register(Long2DoubleSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2DoubleAVLTreeMap();
            var result = new Long2DoubleAVLTreeMap(((Long2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Long2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2DoubleSortedMaps.synchronize((Long2DoubleSortedMap) decoded)));
        register(Long2DoubleMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2DoubleMaps.unmodifiable((Long2DoubleMap) decoded)));
        register(Long2DoubleMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2DoubleMaps.unmodifiable((Long2DoubleMap) decoded)));
        register(Long2DoubleMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Long2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2DoubleMaps.unmodifiable((Long2DoubleMap) decoded)));
        register(Long2DoubleMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2DoubleMaps.synchronize((Long2DoubleMap) decoded)));
        register(Long2ObjectMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ObjectMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2ObjectOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ObjectOpenHashMap() : ((Long2ObjectOpenHashMap) current).clone()));
        register(Long2ObjectArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ObjectArrayMap() : ((Long2ObjectArrayMap) current).clone()));
        register(Long2ObjectLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Long2ObjectLinkedOpenHashMap() : ((Long2ObjectLinkedOpenHashMap) current).clone()));
        register(Long2ObjectAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ObjectAVLTreeMap() : ((Long2ObjectAVLTreeMap) current).clone()));
        register(Long2ObjectRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ObjectRBTreeMap() : ((Long2ObjectRBTreeMap) current).clone()));
        register(Long2ObjectOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Long2ObjectOpenCustomHashMap) current).clone()));
        register(Long2ObjectSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ObjectAVLTreeMap();
            var result = new Long2ObjectAVLTreeMap(((Long2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2ObjectSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ObjectAVLTreeMap();
            var result = new Long2ObjectAVLTreeMap(((Long2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ObjectSortedMaps.unmodifiable((Long2ObjectSortedMap) decoded)));
        register(Long2ObjectSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ObjectAVLTreeMap();
            var result = new Long2ObjectAVLTreeMap(((Long2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ObjectSortedMaps.unmodifiable((Long2ObjectSortedMap) decoded)));
        register(Long2ObjectSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ObjectAVLTreeMap();
            var result = new Long2ObjectAVLTreeMap(((Long2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ObjectSortedMaps.unmodifiable((Long2ObjectSortedMap) decoded)));
        register(Long2ObjectSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ObjectAVLTreeMap();
            var result = new Long2ObjectAVLTreeMap(((Long2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ObjectSortedMaps.synchronize((Long2ObjectSortedMap) decoded)));
        register(Long2ObjectMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ObjectMaps.unmodifiable((Long2ObjectMap) decoded)));
        register(Long2ObjectMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ObjectMaps.unmodifiable((Long2ObjectMap) decoded)));
        register(Long2ObjectMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Long2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ObjectMaps.unmodifiable((Long2ObjectMap) decoded)));
        register(Long2ObjectMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ObjectMaps.synchronize((Long2ObjectMap) decoded)));
        register(Long2ReferenceMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ReferenceMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2ReferenceOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Long2ReferenceOpenHashMap() : ((Long2ReferenceOpenHashMap) current).clone()));
        register(Long2ReferenceArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ReferenceArrayMap() : ((Long2ReferenceArrayMap) current).clone()));
        register(Long2ReferenceLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Long2ReferenceLinkedOpenHashMap() : ((Long2ReferenceLinkedOpenHashMap) current).clone()));
        register(Long2ReferenceAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ReferenceAVLTreeMap() : ((Long2ReferenceAVLTreeMap) current).clone()));
        register(Long2ReferenceRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Long2ReferenceRBTreeMap() : ((Long2ReferenceRBTreeMap) current).clone()));
        register(Long2ReferenceOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Long2ReferenceOpenCustomHashMap) current).clone()));
        register(Long2ReferenceSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ReferenceAVLTreeMap();
            var result = new Long2ReferenceAVLTreeMap(((Long2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Long2ReferenceSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ReferenceAVLTreeMap();
            var result = new Long2ReferenceAVLTreeMap(((Long2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ReferenceSortedMaps.unmodifiable((Long2ReferenceSortedMap) decoded)));
        register(Long2ReferenceSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ReferenceAVLTreeMap();
            var result = new Long2ReferenceAVLTreeMap(((Long2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ReferenceSortedMaps.unmodifiable((Long2ReferenceSortedMap) decoded)));
        register(Long2ReferenceSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ReferenceAVLTreeMap();
            var result = new Long2ReferenceAVLTreeMap(((Long2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ReferenceSortedMaps.unmodifiable((Long2ReferenceSortedMap) decoded)));
        register(Long2ReferenceSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Long2ReferenceAVLTreeMap();
            var result = new Long2ReferenceAVLTreeMap(((Long2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Long2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ReferenceSortedMaps.synchronize((Long2ReferenceSortedMap) decoded)));
        register(Long2ReferenceMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ReferenceMaps.unmodifiable((Long2ReferenceMap) decoded)));
        register(Long2ReferenceMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ReferenceMaps.unmodifiable((Long2ReferenceMap) decoded)));
        register(Long2ReferenceMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Long2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ReferenceMaps.unmodifiable((Long2ReferenceMap) decoded)));
        register(Long2ReferenceMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Long2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Long2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Long2ReferenceMaps.synchronize((Long2ReferenceMap) decoded)));
        register(LongCollection.class, (type, current) -> new Construction(() -> new LongArrayList()));
        register(LongList.class, (type, current) -> new Construction(() -> new LongArrayList()));
        register(LongSet.class, (type, current) -> new Construction(() -> new LongOpenHashSet()));
        register(LongSortedSet.class, (type, current) -> new Construction(() -> current == null ? new LongRBTreeSet() :
                new LongRBTreeSet(((LongSortedSet) current).comparator())));
        register(LongBigList.class, (type, current) -> new Construction(() -> new LongBigArrayBigList()));
        register(LongArrayList.class, (type, current) -> new Construction(
                () -> current == null ? new LongArrayList() : ((LongArrayList) current).clone()));
        register(LongArraySet.class, (type, current) -> new Construction(
                () -> current == null ? new LongArraySet() : ((LongArraySet) current).clone()));
        register(LongOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new LongOpenHashSet() : ((LongOpenHashSet) current).clone()));
        register(LongLinkedOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new LongLinkedOpenHashSet() : ((LongLinkedOpenHashSet) current).clone()));
        register(LongAVLTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new LongAVLTreeSet() : ((LongAVLTreeSet) current).clone()));
        register(LongRBTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new LongRBTreeSet() : ((LongRBTreeSet) current).clone()));
        register(LongBigArrayBigList.class, (type, current) -> new Construction(
                () -> current == null ? new LongBigArrayBigList() : ((LongBigArrayBigList) current).clone()));
        register(LongOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((LongOpenCustomHashSet) current).clone()));
        register(LongLinkedOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((LongLinkedOpenCustomHashSet) current).clone()));
        register(LongCollections.unmodifiable(new LongArrayList()).getClass(),
                (type, current) -> new Construction(() -> new LongArrayList(),
                        decoded -> LongCollections.unmodifiable((LongCollection) decoded)));
        register(LongCollections.unmodifiable(new LongArrayList()).getClass(),
                (type, current) -> new Construction(() -> new LongArrayList(),
                        decoded -> LongCollections.unmodifiable((LongCollection) decoded)));
        register(LongCollections.synchronize(new LongArrayList()).getClass(),
                (type, current) -> new Construction(() -> new LongArrayList(),
                        decoded -> LongCollections.synchronize((LongCollection) decoded)));
        register(LongLists.UnmodifiableList.class, (type, current) -> new Construction(() -> new LongArrayList(),
                decoded -> LongLists.unmodifiable((LongList) decoded)));
        register(LongLists.EmptyList.class, (type, current) -> new Construction(() -> new LongArrayList(),
                decoded -> LongLists.unmodifiable((LongList) decoded)));
        register(LongLists.Singleton.class, (type, current) -> new Construction(() -> new LongArrayList(),
                decoded -> LongLists.unmodifiable((LongList) decoded)));
        register(LongLists.SynchronizedList.class, (type, current) -> new Construction(() -> new LongArrayList(),
                decoded -> LongLists.synchronize((LongList) decoded)));
        register(LongLists.UnmodifiableRandomAccessList.class,
                (type, current) -> new Construction(() -> new LongArrayList(),
                        decoded -> LongLists.unmodifiable((LongList) decoded)));
        register(LongLists.SynchronizedRandomAccessList.class,
                (type, current) -> new Construction(() -> new LongArrayList(),
                        decoded -> LongLists.synchronize((LongList) decoded)));
        register(LongSets.UnmodifiableSet.class, (type, current) -> new Construction(() -> new LongOpenHashSet(),
                decoded -> LongSets.unmodifiable((LongSet) decoded)));
        register(LongSets.EmptySet.class, (type, current) -> new Construction(() -> new LongOpenHashSet(),
                decoded -> LongSets.unmodifiable((LongSet) decoded)));
        register(LongSets.Singleton.class, (type, current) -> new Construction(() -> new LongOpenHashSet(),
                decoded -> LongSets.unmodifiable((LongSet) decoded)));
        register(LongSets.SynchronizedSet.class, (type, current) -> new Construction(() -> new LongOpenHashSet(),
                decoded -> LongSets.synchronize((LongSet) decoded)));
        register(LongSortedSets.UnmodifiableSortedSet.class,
                (type, current) -> new Construction(
                        () -> new LongRBTreeSet(current == null ? null : ((LongSortedSet) current).comparator()),
                        decoded -> LongSortedSets.unmodifiable((LongSortedSet) decoded)));
        register(LongSortedSets.EmptySet.class,
                (type, current) -> new Construction(
                        () -> new LongRBTreeSet(current == null ? null : ((LongSortedSet) current).comparator()),
                        decoded -> LongSortedSets.unmodifiable((LongSortedSet) decoded)));
        register(LongSortedSets.Singleton.class,
                (type, current) -> new Construction(
                        () -> new LongRBTreeSet(current == null ? null : ((LongSortedSet) current).comparator()),
                        decoded -> LongSortedSets.unmodifiable((LongSortedSet) decoded)));
        register(LongSortedSets.SynchronizedSortedSet.class,
                (type, current) -> new Construction(
                        () -> new LongRBTreeSet(current == null ? null : ((LongSortedSet) current).comparator()),
                        decoded -> LongSortedSets.synchronize((LongSortedSet) decoded)));
        register(LongBigLists.UnmodifiableBigList.class,
                (type, current) -> new Construction(() -> new LongBigArrayBigList(),
                        decoded -> LongBigLists.unmodifiable((LongBigList) decoded)));
        register(LongBigLists.EmptyBigList.class, (type, current) -> new Construction(() -> new LongBigArrayBigList(),
                decoded -> LongBigLists.unmodifiable((LongBigList) decoded)));
        register(LongBigLists.Singleton.class, (type, current) -> new Construction(() -> new LongBigArrayBigList(),
                decoded -> LongBigLists.unmodifiable((LongBigList) decoded)));
        register(LongBigLists.SynchronizedBigList.class,
                (type, current) -> new Construction(() -> new LongBigArrayBigList(),
                        decoded -> LongBigLists.synchronize((LongBigList) decoded)));
        register(LongImmutableList.class, (type, current) -> new Construction(() -> new LongArrayList(),
                decoded -> new LongImmutableList((Collection) decoded)));
        register(new LongImmutableList(new long[] { 0, 1 }).subList(0, 1).getClass(),
                (type, current) -> new Construction(() -> new LongArrayList(),
                        decoded -> new LongImmutableList((Collection) decoded)));
        register(LongMappedBigList.class, (type, current) -> new Construction(() -> new LongBigArrayBigList(),
                decoded -> updateMapped((BigList) current, (BigList) decoded)));
    }

    private static void registerFloat() {
        register(Float2BooleanMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2BooleanMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2BooleanOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2BooleanOpenHashMap() : ((Float2BooleanOpenHashMap) current).clone()));
        register(Float2BooleanArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2BooleanArrayMap() : ((Float2BooleanArrayMap) current).clone()));
        register(Float2BooleanLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2BooleanLinkedOpenHashMap() : ((Float2BooleanLinkedOpenHashMap) current).clone()));
        register(Float2BooleanAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2BooleanAVLTreeMap() : ((Float2BooleanAVLTreeMap) current).clone()));
        register(Float2BooleanRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2BooleanRBTreeMap() : ((Float2BooleanRBTreeMap) current).clone()));
        register(Float2BooleanOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Float2BooleanOpenCustomHashMap) current).clone()));
        register(Float2BooleanSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2BooleanAVLTreeMap();
            var result = new Float2BooleanAVLTreeMap(((Float2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Float2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2BooleanSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2BooleanAVLTreeMap();
            var result = new Float2BooleanAVLTreeMap(((Float2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Float2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2BooleanSortedMaps.unmodifiable((Float2BooleanSortedMap) decoded)));
        register(Float2BooleanSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2BooleanAVLTreeMap();
            var result = new Float2BooleanAVLTreeMap(((Float2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Float2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2BooleanSortedMaps.unmodifiable((Float2BooleanSortedMap) decoded)));
        register(Float2BooleanSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2BooleanAVLTreeMap();
            var result = new Float2BooleanAVLTreeMap(((Float2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Float2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2BooleanSortedMaps.unmodifiable((Float2BooleanSortedMap) decoded)));
        register(Float2BooleanSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2BooleanAVLTreeMap();
            var result = new Float2BooleanAVLTreeMap(((Float2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Float2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2BooleanSortedMaps.synchronize((Float2BooleanSortedMap) decoded)));
        register(Float2BooleanMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2BooleanMaps.unmodifiable((Float2BooleanMap) decoded)));
        register(Float2BooleanMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2BooleanMaps.unmodifiable((Float2BooleanMap) decoded)));
        register(Float2BooleanMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Float2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2BooleanMaps.unmodifiable((Float2BooleanMap) decoded)));
        register(Float2BooleanMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2BooleanMaps.synchronize((Float2BooleanMap) decoded)));
        register(Float2ByteMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ByteMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2ByteOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ByteOpenHashMap() : ((Float2ByteOpenHashMap) current).clone()));
        register(Float2ByteArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ByteArrayMap() : ((Float2ByteArrayMap) current).clone()));
        register(Float2ByteLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2ByteLinkedOpenHashMap() : ((Float2ByteLinkedOpenHashMap) current).clone()));
        register(Float2ByteAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ByteAVLTreeMap() : ((Float2ByteAVLTreeMap) current).clone()));
        register(Float2ByteRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ByteRBTreeMap() : ((Float2ByteRBTreeMap) current).clone()));
        register(Float2ByteOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Float2ByteOpenCustomHashMap) current).clone()));
        register(Float2ByteSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ByteAVLTreeMap();
            var result = new Float2ByteAVLTreeMap(((Float2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ByteSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2ByteSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ByteAVLTreeMap();
            var result = new Float2ByteAVLTreeMap(((Float2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ByteSortedMaps.unmodifiable((Float2ByteSortedMap) decoded)));
        register(Float2ByteSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ByteAVLTreeMap();
            var result = new Float2ByteAVLTreeMap(((Float2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ByteSortedMaps.unmodifiable((Float2ByteSortedMap) decoded)));
        register(Float2ByteSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ByteAVLTreeMap();
            var result = new Float2ByteAVLTreeMap(((Float2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ByteSortedMaps.unmodifiable((Float2ByteSortedMap) decoded)));
        register(Float2ByteSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ByteAVLTreeMap();
            var result = new Float2ByteAVLTreeMap(((Float2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ByteSortedMaps.synchronize((Float2ByteSortedMap) decoded)));
        register(Float2ByteMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ByteMaps.unmodifiable((Float2ByteMap) decoded)));
        register(Float2ByteMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ByteMaps.unmodifiable((Float2ByteMap) decoded)));
        register(Float2ByteMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Float2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ByteMaps.unmodifiable((Float2ByteMap) decoded)));
        register(Float2ByteMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ByteMaps.synchronize((Float2ByteMap) decoded)));
        register(Float2ShortMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ShortMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2ShortOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ShortOpenHashMap() : ((Float2ShortOpenHashMap) current).clone()));
        register(Float2ShortArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ShortArrayMap() : ((Float2ShortArrayMap) current).clone()));
        register(Float2ShortLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2ShortLinkedOpenHashMap() : ((Float2ShortLinkedOpenHashMap) current).clone()));
        register(Float2ShortAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ShortAVLTreeMap() : ((Float2ShortAVLTreeMap) current).clone()));
        register(Float2ShortRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ShortRBTreeMap() : ((Float2ShortRBTreeMap) current).clone()));
        register(Float2ShortOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Float2ShortOpenCustomHashMap) current).clone()));
        register(Float2ShortSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ShortAVLTreeMap();
            var result = new Float2ShortAVLTreeMap(((Float2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ShortSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2ShortSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ShortAVLTreeMap();
            var result = new Float2ShortAVLTreeMap(((Float2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ShortSortedMaps.unmodifiable((Float2ShortSortedMap) decoded)));
        register(Float2ShortSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ShortAVLTreeMap();
            var result = new Float2ShortAVLTreeMap(((Float2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ShortSortedMaps.unmodifiable((Float2ShortSortedMap) decoded)));
        register(Float2ShortSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ShortAVLTreeMap();
            var result = new Float2ShortAVLTreeMap(((Float2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ShortSortedMaps.unmodifiable((Float2ShortSortedMap) decoded)));
        register(Float2ShortSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ShortAVLTreeMap();
            var result = new Float2ShortAVLTreeMap(((Float2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ShortSortedMaps.synchronize((Float2ShortSortedMap) decoded)));
        register(Float2ShortMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ShortMaps.unmodifiable((Float2ShortMap) decoded)));
        register(Float2ShortMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ShortMaps.unmodifiable((Float2ShortMap) decoded)));
        register(Float2ShortMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Float2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ShortMaps.unmodifiable((Float2ShortMap) decoded)));
        register(Float2ShortMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ShortMaps.synchronize((Float2ShortMap) decoded)));
        register(Float2CharMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2CharMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2CharOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2CharOpenHashMap() : ((Float2CharOpenHashMap) current).clone()));
        register(Float2CharArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2CharArrayMap() : ((Float2CharArrayMap) current).clone()));
        register(Float2CharLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2CharLinkedOpenHashMap() : ((Float2CharLinkedOpenHashMap) current).clone()));
        register(Float2CharAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2CharAVLTreeMap() : ((Float2CharAVLTreeMap) current).clone()));
        register(Float2CharRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2CharRBTreeMap() : ((Float2CharRBTreeMap) current).clone()));
        register(Float2CharOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Float2CharOpenCustomHashMap) current).clone()));
        register(Float2CharSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2CharAVLTreeMap();
            var result = new Float2CharAVLTreeMap(((Float2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Float2CharSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2CharSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2CharAVLTreeMap();
            var result = new Float2CharAVLTreeMap(((Float2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Float2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2CharSortedMaps.unmodifiable((Float2CharSortedMap) decoded)));
        register(Float2CharSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2CharAVLTreeMap();
            var result = new Float2CharAVLTreeMap(((Float2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Float2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2CharSortedMaps.unmodifiable((Float2CharSortedMap) decoded)));
        register(Float2CharSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2CharAVLTreeMap();
            var result = new Float2CharAVLTreeMap(((Float2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Float2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2CharSortedMaps.unmodifiable((Float2CharSortedMap) decoded)));
        register(Float2CharSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2CharAVLTreeMap();
            var result = new Float2CharAVLTreeMap(((Float2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Float2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2CharSortedMaps.synchronize((Float2CharSortedMap) decoded)));
        register(Float2CharMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2CharMaps.unmodifiable((Float2CharMap) decoded)));
        register(Float2CharMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2CharMaps.unmodifiable((Float2CharMap) decoded)));
        register(Float2CharMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Float2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2CharMaps.unmodifiable((Float2CharMap) decoded)));
        register(Float2CharMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2CharMaps.synchronize((Float2CharMap) decoded)));
        register(Float2IntMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2IntMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2IntOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2IntOpenHashMap() : ((Float2IntOpenHashMap) current).clone()));
        register(Float2IntArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2IntArrayMap() : ((Float2IntArrayMap) current).clone()));
        register(Float2IntLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2IntLinkedOpenHashMap() : ((Float2IntLinkedOpenHashMap) current).clone()));
        register(Float2IntAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2IntAVLTreeMap() : ((Float2IntAVLTreeMap) current).clone()));
        register(Float2IntRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2IntRBTreeMap() : ((Float2IntRBTreeMap) current).clone()));
        register(Float2IntOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Float2IntOpenCustomHashMap) current).clone()));
        register(Float2IntSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2IntAVLTreeMap();
            var result = new Float2IntAVLTreeMap(((Float2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Float2IntSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2IntSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2IntAVLTreeMap();
            var result = new Float2IntAVLTreeMap(((Float2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Float2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2IntSortedMaps.unmodifiable((Float2IntSortedMap) decoded)));
        register(Float2IntSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2IntAVLTreeMap();
            var result = new Float2IntAVLTreeMap(((Float2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Float2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2IntSortedMaps.unmodifiable((Float2IntSortedMap) decoded)));
        register(Float2IntSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2IntAVLTreeMap();
            var result = new Float2IntAVLTreeMap(((Float2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Float2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2IntSortedMaps.unmodifiable((Float2IntSortedMap) decoded)));
        register(Float2IntSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2IntAVLTreeMap();
            var result = new Float2IntAVLTreeMap(((Float2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Float2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2IntSortedMaps.synchronize((Float2IntSortedMap) decoded)));
        register(Float2IntMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2IntMaps.unmodifiable((Float2IntMap) decoded)));
        register(Float2IntMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2IntMaps.unmodifiable((Float2IntMap) decoded)));
        register(Float2IntMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Float2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2IntMaps.unmodifiable((Float2IntMap) decoded)));
        register(Float2IntMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2IntMaps.synchronize((Float2IntMap) decoded)));
        register(Float2LongMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2LongMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2LongOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2LongOpenHashMap() : ((Float2LongOpenHashMap) current).clone()));
        register(Float2LongArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2LongArrayMap() : ((Float2LongArrayMap) current).clone()));
        register(Float2LongLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2LongLinkedOpenHashMap() : ((Float2LongLinkedOpenHashMap) current).clone()));
        register(Float2LongAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2LongAVLTreeMap() : ((Float2LongAVLTreeMap) current).clone()));
        register(Float2LongRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2LongRBTreeMap() : ((Float2LongRBTreeMap) current).clone()));
        register(Float2LongOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Float2LongOpenCustomHashMap) current).clone()));
        register(Float2LongSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2LongAVLTreeMap();
            var result = new Float2LongAVLTreeMap(((Float2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Float2LongSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2LongSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2LongAVLTreeMap();
            var result = new Float2LongAVLTreeMap(((Float2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Float2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2LongSortedMaps.unmodifiable((Float2LongSortedMap) decoded)));
        register(Float2LongSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2LongAVLTreeMap();
            var result = new Float2LongAVLTreeMap(((Float2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Float2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2LongSortedMaps.unmodifiable((Float2LongSortedMap) decoded)));
        register(Float2LongSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2LongAVLTreeMap();
            var result = new Float2LongAVLTreeMap(((Float2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Float2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2LongSortedMaps.unmodifiable((Float2LongSortedMap) decoded)));
        register(Float2LongSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2LongAVLTreeMap();
            var result = new Float2LongAVLTreeMap(((Float2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Float2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2LongSortedMaps.synchronize((Float2LongSortedMap) decoded)));
        register(Float2LongMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2LongMaps.unmodifiable((Float2LongMap) decoded)));
        register(Float2LongMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2LongMaps.unmodifiable((Float2LongMap) decoded)));
        register(Float2LongMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Float2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2LongMaps.unmodifiable((Float2LongMap) decoded)));
        register(Float2LongMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2LongMaps.synchronize((Float2LongMap) decoded)));
        register(Float2FloatMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2FloatMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2FloatOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2FloatOpenHashMap() : ((Float2FloatOpenHashMap) current).clone()));
        register(Float2FloatArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2FloatArrayMap() : ((Float2FloatArrayMap) current).clone()));
        register(Float2FloatLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2FloatLinkedOpenHashMap() : ((Float2FloatLinkedOpenHashMap) current).clone()));
        register(Float2FloatAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2FloatAVLTreeMap() : ((Float2FloatAVLTreeMap) current).clone()));
        register(Float2FloatRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2FloatRBTreeMap() : ((Float2FloatRBTreeMap) current).clone()));
        register(Float2FloatOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Float2FloatOpenCustomHashMap) current).clone()));
        register(Float2FloatSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2FloatAVLTreeMap();
            var result = new Float2FloatAVLTreeMap(((Float2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Float2FloatSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2FloatSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2FloatAVLTreeMap();
            var result = new Float2FloatAVLTreeMap(((Float2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Float2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2FloatSortedMaps.unmodifiable((Float2FloatSortedMap) decoded)));
        register(Float2FloatSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2FloatAVLTreeMap();
            var result = new Float2FloatAVLTreeMap(((Float2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Float2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2FloatSortedMaps.unmodifiable((Float2FloatSortedMap) decoded)));
        register(Float2FloatSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2FloatAVLTreeMap();
            var result = new Float2FloatAVLTreeMap(((Float2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Float2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2FloatSortedMaps.unmodifiable((Float2FloatSortedMap) decoded)));
        register(Float2FloatSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2FloatAVLTreeMap();
            var result = new Float2FloatAVLTreeMap(((Float2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Float2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2FloatSortedMaps.synchronize((Float2FloatSortedMap) decoded)));
        register(Float2FloatMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2FloatMaps.unmodifiable((Float2FloatMap) decoded)));
        register(Float2FloatMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2FloatMaps.unmodifiable((Float2FloatMap) decoded)));
        register(Float2FloatMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Float2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2FloatMaps.unmodifiable((Float2FloatMap) decoded)));
        register(Float2FloatMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2FloatMaps.synchronize((Float2FloatMap) decoded)));
        register(Float2DoubleMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2DoubleMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2DoubleOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2DoubleOpenHashMap() : ((Float2DoubleOpenHashMap) current).clone()));
        register(Float2DoubleArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2DoubleArrayMap() : ((Float2DoubleArrayMap) current).clone()));
        register(Float2DoubleLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2DoubleLinkedOpenHashMap() : ((Float2DoubleLinkedOpenHashMap) current).clone()));
        register(Float2DoubleAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2DoubleAVLTreeMap() : ((Float2DoubleAVLTreeMap) current).clone()));
        register(Float2DoubleRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2DoubleRBTreeMap() : ((Float2DoubleRBTreeMap) current).clone()));
        register(Float2DoubleOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Float2DoubleOpenCustomHashMap) current).clone()));
        register(Float2DoubleSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2DoubleAVLTreeMap();
            var result = new Float2DoubleAVLTreeMap(((Float2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Float2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2DoubleSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2DoubleAVLTreeMap();
            var result = new Float2DoubleAVLTreeMap(((Float2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Float2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2DoubleSortedMaps.unmodifiable((Float2DoubleSortedMap) decoded)));
        register(Float2DoubleSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2DoubleAVLTreeMap();
            var result = new Float2DoubleAVLTreeMap(((Float2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Float2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2DoubleSortedMaps.unmodifiable((Float2DoubleSortedMap) decoded)));
        register(Float2DoubleSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2DoubleAVLTreeMap();
            var result = new Float2DoubleAVLTreeMap(((Float2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Float2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2DoubleSortedMaps.unmodifiable((Float2DoubleSortedMap) decoded)));
        register(Float2DoubleSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2DoubleAVLTreeMap();
            var result = new Float2DoubleAVLTreeMap(((Float2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Float2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2DoubleSortedMaps.synchronize((Float2DoubleSortedMap) decoded)));
        register(Float2DoubleMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2DoubleMaps.unmodifiable((Float2DoubleMap) decoded)));
        register(Float2DoubleMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2DoubleMaps.unmodifiable((Float2DoubleMap) decoded)));
        register(Float2DoubleMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Float2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2DoubleMaps.unmodifiable((Float2DoubleMap) decoded)));
        register(Float2DoubleMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2DoubleMaps.synchronize((Float2DoubleMap) decoded)));
        register(Float2ObjectMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ObjectMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2ObjectOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ObjectOpenHashMap() : ((Float2ObjectOpenHashMap) current).clone()));
        register(Float2ObjectArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ObjectArrayMap() : ((Float2ObjectArrayMap) current).clone()));
        register(Float2ObjectLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2ObjectLinkedOpenHashMap() : ((Float2ObjectLinkedOpenHashMap) current).clone()));
        register(Float2ObjectAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ObjectAVLTreeMap() : ((Float2ObjectAVLTreeMap) current).clone()));
        register(Float2ObjectRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ObjectRBTreeMap() : ((Float2ObjectRBTreeMap) current).clone()));
        register(Float2ObjectOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Float2ObjectOpenCustomHashMap) current).clone()));
        register(Float2ObjectSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ObjectAVLTreeMap();
            var result = new Float2ObjectAVLTreeMap(((Float2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2ObjectSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ObjectAVLTreeMap();
            var result = new Float2ObjectAVLTreeMap(((Float2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ObjectSortedMaps.unmodifiable((Float2ObjectSortedMap) decoded)));
        register(Float2ObjectSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ObjectAVLTreeMap();
            var result = new Float2ObjectAVLTreeMap(((Float2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ObjectSortedMaps.unmodifiable((Float2ObjectSortedMap) decoded)));
        register(Float2ObjectSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ObjectAVLTreeMap();
            var result = new Float2ObjectAVLTreeMap(((Float2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ObjectSortedMaps.unmodifiable((Float2ObjectSortedMap) decoded)));
        register(Float2ObjectSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ObjectAVLTreeMap();
            var result = new Float2ObjectAVLTreeMap(((Float2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ObjectSortedMaps.synchronize((Float2ObjectSortedMap) decoded)));
        register(Float2ObjectMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ObjectMaps.unmodifiable((Float2ObjectMap) decoded)));
        register(Float2ObjectMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ObjectMaps.unmodifiable((Float2ObjectMap) decoded)));
        register(Float2ObjectMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Float2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ObjectMaps.unmodifiable((Float2ObjectMap) decoded)));
        register(Float2ObjectMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ObjectMaps.synchronize((Float2ObjectMap) decoded)));
        register(Float2ReferenceMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ReferenceMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2ReferenceOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2ReferenceOpenHashMap() : ((Float2ReferenceOpenHashMap) current).clone()));
        register(Float2ReferenceArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ReferenceArrayMap() : ((Float2ReferenceArrayMap) current).clone()));
        register(Float2ReferenceLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2ReferenceLinkedOpenHashMap() : ((Float2ReferenceLinkedOpenHashMap) current).clone()));
        register(Float2ReferenceAVLTreeMap.class, (type, current) -> new Construction(() -> current == null ?
                new Float2ReferenceAVLTreeMap() : ((Float2ReferenceAVLTreeMap) current).clone()));
        register(Float2ReferenceRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Float2ReferenceRBTreeMap() : ((Float2ReferenceRBTreeMap) current).clone()));
        register(Float2ReferenceOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Float2ReferenceOpenCustomHashMap) current).clone()));
        register(Float2ReferenceSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ReferenceAVLTreeMap();
            var result = new Float2ReferenceAVLTreeMap(((Float2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Float2ReferenceSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ReferenceAVLTreeMap();
            var result = new Float2ReferenceAVLTreeMap(((Float2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ReferenceSortedMaps.unmodifiable((Float2ReferenceSortedMap) decoded)));
        register(Float2ReferenceSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ReferenceAVLTreeMap();
            var result = new Float2ReferenceAVLTreeMap(((Float2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ReferenceSortedMaps.unmodifiable((Float2ReferenceSortedMap) decoded)));
        register(Float2ReferenceSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ReferenceAVLTreeMap();
            var result = new Float2ReferenceAVLTreeMap(((Float2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ReferenceSortedMaps.unmodifiable((Float2ReferenceSortedMap) decoded)));
        register(Float2ReferenceSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Float2ReferenceAVLTreeMap();
            var result = new Float2ReferenceAVLTreeMap(((Float2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Float2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ReferenceSortedMaps.synchronize((Float2ReferenceSortedMap) decoded)));
        register(Float2ReferenceMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ReferenceMaps.unmodifiable((Float2ReferenceMap) decoded)));
        register(Float2ReferenceMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ReferenceMaps.unmodifiable((Float2ReferenceMap) decoded)));
        register(Float2ReferenceMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Float2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ReferenceMaps.unmodifiable((Float2ReferenceMap) decoded)));
        register(Float2ReferenceMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Float2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Float2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Float2ReferenceMaps.synchronize((Float2ReferenceMap) decoded)));
        register(FloatCollection.class, (type, current) -> new Construction(() -> new FloatArrayList()));
        register(FloatList.class, (type, current) -> new Construction(() -> new FloatArrayList()));
        register(FloatSet.class, (type, current) -> new Construction(() -> new FloatOpenHashSet()));
        register(FloatSortedSet.class, (type, current) -> new Construction(() -> current == null ?
                new FloatRBTreeSet() : new FloatRBTreeSet(((FloatSortedSet) current).comparator())));
        register(FloatBigList.class, (type, current) -> new Construction(() -> new FloatBigArrayBigList()));
        register(FloatArrayList.class, (type, current) -> new Construction(
                () -> current == null ? new FloatArrayList() : ((FloatArrayList) current).clone()));
        register(FloatArraySet.class, (type, current) -> new Construction(
                () -> current == null ? new FloatArraySet() : ((FloatArraySet) current).clone()));
        register(FloatOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new FloatOpenHashSet() : ((FloatOpenHashSet) current).clone()));
        register(FloatLinkedOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new FloatLinkedOpenHashSet() : ((FloatLinkedOpenHashSet) current).clone()));
        register(FloatAVLTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new FloatAVLTreeSet() : ((FloatAVLTreeSet) current).clone()));
        register(FloatRBTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new FloatRBTreeSet() : ((FloatRBTreeSet) current).clone()));
        register(FloatBigArrayBigList.class, (type, current) -> new Construction(
                () -> current == null ? new FloatBigArrayBigList() : ((FloatBigArrayBigList) current).clone()));
        register(FloatOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((FloatOpenCustomHashSet) current).clone()));
        register(FloatLinkedOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((FloatLinkedOpenCustomHashSet) current).clone()));
        register(FloatCollections.unmodifiable(new FloatArrayList()).getClass(),
                (type, current) -> new Construction(() -> new FloatArrayList(),
                        decoded -> FloatCollections.unmodifiable((FloatCollection) decoded)));
        register(FloatCollections.unmodifiable(new FloatArrayList()).getClass(),
                (type, current) -> new Construction(() -> new FloatArrayList(),
                        decoded -> FloatCollections.unmodifiable((FloatCollection) decoded)));
        register(FloatCollections.synchronize(new FloatArrayList()).getClass(),
                (type, current) -> new Construction(() -> new FloatArrayList(),
                        decoded -> FloatCollections.synchronize((FloatCollection) decoded)));
        register(FloatLists.UnmodifiableList.class, (type, current) -> new Construction(() -> new FloatArrayList(),
                decoded -> FloatLists.unmodifiable((FloatList) decoded)));
        register(FloatLists.EmptyList.class, (type, current) -> new Construction(() -> new FloatArrayList(),
                decoded -> FloatLists.unmodifiable((FloatList) decoded)));
        register(FloatLists.Singleton.class, (type, current) -> new Construction(() -> new FloatArrayList(),
                decoded -> FloatLists.unmodifiable((FloatList) decoded)));
        register(FloatLists.SynchronizedList.class, (type, current) -> new Construction(() -> new FloatArrayList(),
                decoded -> FloatLists.synchronize((FloatList) decoded)));
        register(FloatLists.UnmodifiableRandomAccessList.class,
                (type, current) -> new Construction(() -> new FloatArrayList(),
                        decoded -> FloatLists.unmodifiable((FloatList) decoded)));
        register(FloatLists.SynchronizedRandomAccessList.class,
                (type, current) -> new Construction(() -> new FloatArrayList(),
                        decoded -> FloatLists.synchronize((FloatList) decoded)));
        register(FloatSets.UnmodifiableSet.class, (type, current) -> new Construction(() -> new FloatOpenHashSet(),
                decoded -> FloatSets.unmodifiable((FloatSet) decoded)));
        register(FloatSets.EmptySet.class, (type, current) -> new Construction(() -> new FloatOpenHashSet(),
                decoded -> FloatSets.unmodifiable((FloatSet) decoded)));
        register(FloatSets.Singleton.class, (type, current) -> new Construction(() -> new FloatOpenHashSet(),
                decoded -> FloatSets.unmodifiable((FloatSet) decoded)));
        register(FloatSets.SynchronizedSet.class, (type, current) -> new Construction(() -> new FloatOpenHashSet(),
                decoded -> FloatSets.synchronize((FloatSet) decoded)));
        register(FloatSortedSets.UnmodifiableSortedSet.class,
                (type, current) -> new Construction(
                        () -> new FloatRBTreeSet(current == null ? null : ((FloatSortedSet) current).comparator()),
                        decoded -> FloatSortedSets.unmodifiable((FloatSortedSet) decoded)));
        register(FloatSortedSets.EmptySet.class,
                (type, current) -> new Construction(
                        () -> new FloatRBTreeSet(current == null ? null : ((FloatSortedSet) current).comparator()),
                        decoded -> FloatSortedSets.unmodifiable((FloatSortedSet) decoded)));
        register(FloatSortedSets.Singleton.class,
                (type, current) -> new Construction(
                        () -> new FloatRBTreeSet(current == null ? null : ((FloatSortedSet) current).comparator()),
                        decoded -> FloatSortedSets.unmodifiable((FloatSortedSet) decoded)));
        register(FloatSortedSets.SynchronizedSortedSet.class,
                (type, current) -> new Construction(
                        () -> new FloatRBTreeSet(current == null ? null : ((FloatSortedSet) current).comparator()),
                        decoded -> FloatSortedSets.synchronize((FloatSortedSet) decoded)));
        register(FloatBigLists.UnmodifiableBigList.class,
                (type, current) -> new Construction(() -> new FloatBigArrayBigList(),
                        decoded -> FloatBigLists.unmodifiable((FloatBigList) decoded)));
        register(FloatBigLists.EmptyBigList.class, (type, current) -> new Construction(() -> new FloatBigArrayBigList(),
                decoded -> FloatBigLists.unmodifiable((FloatBigList) decoded)));
        register(FloatBigLists.Singleton.class, (type, current) -> new Construction(() -> new FloatBigArrayBigList(),
                decoded -> FloatBigLists.unmodifiable((FloatBigList) decoded)));
        register(FloatBigLists.SynchronizedBigList.class,
                (type, current) -> new Construction(() -> new FloatBigArrayBigList(),
                        decoded -> FloatBigLists.synchronize((FloatBigList) decoded)));
        register(FloatImmutableList.class, (type, current) -> new Construction(() -> new FloatArrayList(),
                decoded -> new FloatImmutableList((Collection) decoded)));
        register(new FloatImmutableList(new float[] { 0, 1 }).subList(0, 1).getClass(),
                (type, current) -> new Construction(() -> new FloatArrayList(),
                        decoded -> new FloatImmutableList((Collection) decoded)));
        register(FloatMappedBigList.class, (type, current) -> new Construction(() -> new FloatBigArrayBigList(),
                decoded -> updateMapped((BigList) current, (BigList) decoded)));
    }

    private static void registerDouble() {
        register(Double2BooleanMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2BooleanMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2BooleanOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2BooleanOpenHashMap() : ((Double2BooleanOpenHashMap) current).clone()));
        register(Double2BooleanArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2BooleanArrayMap() : ((Double2BooleanArrayMap) current).clone()));
        register(Double2BooleanLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2BooleanLinkedOpenHashMap() : ((Double2BooleanLinkedOpenHashMap) current).clone()));
        register(Double2BooleanAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2BooleanAVLTreeMap() : ((Double2BooleanAVLTreeMap) current).clone()));
        register(Double2BooleanRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2BooleanRBTreeMap() : ((Double2BooleanRBTreeMap) current).clone()));
        register(Double2BooleanOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Double2BooleanOpenCustomHashMap) current).clone()));
        register(Double2BooleanSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2BooleanAVLTreeMap();
            var result = new Double2BooleanAVLTreeMap(((Double2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Double2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2BooleanSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2BooleanAVLTreeMap();
            var result = new Double2BooleanAVLTreeMap(((Double2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Double2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2BooleanSortedMaps.unmodifiable((Double2BooleanSortedMap) decoded)));
        register(Double2BooleanSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2BooleanAVLTreeMap();
            var result = new Double2BooleanAVLTreeMap(((Double2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Double2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2BooleanSortedMaps.unmodifiable((Double2BooleanSortedMap) decoded)));
        register(Double2BooleanSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2BooleanAVLTreeMap();
            var result = new Double2BooleanAVLTreeMap(((Double2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Double2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2BooleanSortedMaps.unmodifiable((Double2BooleanSortedMap) decoded)));
        register(Double2BooleanSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2BooleanAVLTreeMap();
            var result = new Double2BooleanAVLTreeMap(((Double2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Double2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2BooleanSortedMaps.synchronize((Double2BooleanSortedMap) decoded)));
        register(Double2BooleanMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2BooleanMaps.unmodifiable((Double2BooleanMap) decoded)));
        register(Double2BooleanMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2BooleanMaps.unmodifiable((Double2BooleanMap) decoded)));
        register(Double2BooleanMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Double2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2BooleanMaps.unmodifiable((Double2BooleanMap) decoded)));
        register(Double2BooleanMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2BooleanMaps.synchronize((Double2BooleanMap) decoded)));
        register(Double2ByteMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ByteMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2ByteOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ByteOpenHashMap() : ((Double2ByteOpenHashMap) current).clone()));
        register(Double2ByteArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ByteArrayMap() : ((Double2ByteArrayMap) current).clone()));
        register(Double2ByteLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2ByteLinkedOpenHashMap() : ((Double2ByteLinkedOpenHashMap) current).clone()));
        register(Double2ByteAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ByteAVLTreeMap() : ((Double2ByteAVLTreeMap) current).clone()));
        register(Double2ByteRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ByteRBTreeMap() : ((Double2ByteRBTreeMap) current).clone()));
        register(Double2ByteOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Double2ByteOpenCustomHashMap) current).clone()));
        register(Double2ByteSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ByteAVLTreeMap();
            var result = new Double2ByteAVLTreeMap(((Double2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ByteSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2ByteSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ByteAVLTreeMap();
            var result = new Double2ByteAVLTreeMap(((Double2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ByteSortedMaps.unmodifiable((Double2ByteSortedMap) decoded)));
        register(Double2ByteSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ByteAVLTreeMap();
            var result = new Double2ByteAVLTreeMap(((Double2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ByteSortedMaps.unmodifiable((Double2ByteSortedMap) decoded)));
        register(Double2ByteSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ByteAVLTreeMap();
            var result = new Double2ByteAVLTreeMap(((Double2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ByteSortedMaps.unmodifiable((Double2ByteSortedMap) decoded)));
        register(Double2ByteSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ByteAVLTreeMap();
            var result = new Double2ByteAVLTreeMap(((Double2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ByteSortedMaps.synchronize((Double2ByteSortedMap) decoded)));
        register(Double2ByteMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ByteMaps.unmodifiable((Double2ByteMap) decoded)));
        register(Double2ByteMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ByteMaps.unmodifiable((Double2ByteMap) decoded)));
        register(Double2ByteMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Double2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ByteMaps.unmodifiable((Double2ByteMap) decoded)));
        register(Double2ByteMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ByteMaps.synchronize((Double2ByteMap) decoded)));
        register(Double2ShortMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ShortMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2ShortOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ShortOpenHashMap() : ((Double2ShortOpenHashMap) current).clone()));
        register(Double2ShortArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ShortArrayMap() : ((Double2ShortArrayMap) current).clone()));
        register(Double2ShortLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2ShortLinkedOpenHashMap() : ((Double2ShortLinkedOpenHashMap) current).clone()));
        register(Double2ShortAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ShortAVLTreeMap() : ((Double2ShortAVLTreeMap) current).clone()));
        register(Double2ShortRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ShortRBTreeMap() : ((Double2ShortRBTreeMap) current).clone()));
        register(Double2ShortOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Double2ShortOpenCustomHashMap) current).clone()));
        register(Double2ShortSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ShortAVLTreeMap();
            var result = new Double2ShortAVLTreeMap(((Double2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ShortSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2ShortSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ShortAVLTreeMap();
            var result = new Double2ShortAVLTreeMap(((Double2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ShortSortedMaps.unmodifiable((Double2ShortSortedMap) decoded)));
        register(Double2ShortSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ShortAVLTreeMap();
            var result = new Double2ShortAVLTreeMap(((Double2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ShortSortedMaps.unmodifiable((Double2ShortSortedMap) decoded)));
        register(Double2ShortSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ShortAVLTreeMap();
            var result = new Double2ShortAVLTreeMap(((Double2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ShortSortedMaps.unmodifiable((Double2ShortSortedMap) decoded)));
        register(Double2ShortSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ShortAVLTreeMap();
            var result = new Double2ShortAVLTreeMap(((Double2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ShortSortedMaps.synchronize((Double2ShortSortedMap) decoded)));
        register(Double2ShortMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ShortMaps.unmodifiable((Double2ShortMap) decoded)));
        register(Double2ShortMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ShortMaps.unmodifiable((Double2ShortMap) decoded)));
        register(Double2ShortMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Double2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ShortMaps.unmodifiable((Double2ShortMap) decoded)));
        register(Double2ShortMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ShortMaps.synchronize((Double2ShortMap) decoded)));
        register(Double2CharMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2CharMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2CharOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2CharOpenHashMap() : ((Double2CharOpenHashMap) current).clone()));
        register(Double2CharArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2CharArrayMap() : ((Double2CharArrayMap) current).clone()));
        register(Double2CharLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2CharLinkedOpenHashMap() : ((Double2CharLinkedOpenHashMap) current).clone()));
        register(Double2CharAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2CharAVLTreeMap() : ((Double2CharAVLTreeMap) current).clone()));
        register(Double2CharRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2CharRBTreeMap() : ((Double2CharRBTreeMap) current).clone()));
        register(Double2CharOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Double2CharOpenCustomHashMap) current).clone()));
        register(Double2CharSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2CharAVLTreeMap();
            var result = new Double2CharAVLTreeMap(((Double2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Double2CharSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2CharSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2CharAVLTreeMap();
            var result = new Double2CharAVLTreeMap(((Double2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Double2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2CharSortedMaps.unmodifiable((Double2CharSortedMap) decoded)));
        register(Double2CharSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2CharAVLTreeMap();
            var result = new Double2CharAVLTreeMap(((Double2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Double2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2CharSortedMaps.unmodifiable((Double2CharSortedMap) decoded)));
        register(Double2CharSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2CharAVLTreeMap();
            var result = new Double2CharAVLTreeMap(((Double2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Double2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2CharSortedMaps.unmodifiable((Double2CharSortedMap) decoded)));
        register(Double2CharSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2CharAVLTreeMap();
            var result = new Double2CharAVLTreeMap(((Double2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Double2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2CharSortedMaps.synchronize((Double2CharSortedMap) decoded)));
        register(Double2CharMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2CharMaps.unmodifiable((Double2CharMap) decoded)));
        register(Double2CharMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2CharMaps.unmodifiable((Double2CharMap) decoded)));
        register(Double2CharMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Double2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2CharMaps.unmodifiable((Double2CharMap) decoded)));
        register(Double2CharMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2CharMaps.synchronize((Double2CharMap) decoded)));
        register(Double2IntMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2IntMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2IntOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2IntOpenHashMap() : ((Double2IntOpenHashMap) current).clone()));
        register(Double2IntArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2IntArrayMap() : ((Double2IntArrayMap) current).clone()));
        register(Double2IntLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2IntLinkedOpenHashMap() : ((Double2IntLinkedOpenHashMap) current).clone()));
        register(Double2IntAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2IntAVLTreeMap() : ((Double2IntAVLTreeMap) current).clone()));
        register(Double2IntRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2IntRBTreeMap() : ((Double2IntRBTreeMap) current).clone()));
        register(Double2IntOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Double2IntOpenCustomHashMap) current).clone()));
        register(Double2IntSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2IntAVLTreeMap();
            var result = new Double2IntAVLTreeMap(((Double2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Double2IntSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2IntSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2IntAVLTreeMap();
            var result = new Double2IntAVLTreeMap(((Double2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Double2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2IntSortedMaps.unmodifiable((Double2IntSortedMap) decoded)));
        register(Double2IntSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2IntAVLTreeMap();
            var result = new Double2IntAVLTreeMap(((Double2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Double2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2IntSortedMaps.unmodifiable((Double2IntSortedMap) decoded)));
        register(Double2IntSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2IntAVLTreeMap();
            var result = new Double2IntAVLTreeMap(((Double2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Double2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2IntSortedMaps.unmodifiable((Double2IntSortedMap) decoded)));
        register(Double2IntSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2IntAVLTreeMap();
            var result = new Double2IntAVLTreeMap(((Double2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Double2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2IntSortedMaps.synchronize((Double2IntSortedMap) decoded)));
        register(Double2IntMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2IntMaps.unmodifiable((Double2IntMap) decoded)));
        register(Double2IntMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2IntMaps.unmodifiable((Double2IntMap) decoded)));
        register(Double2IntMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Double2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2IntMaps.unmodifiable((Double2IntMap) decoded)));
        register(Double2IntMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2IntMaps.synchronize((Double2IntMap) decoded)));
        register(Double2LongMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2LongMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2LongOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2LongOpenHashMap() : ((Double2LongOpenHashMap) current).clone()));
        register(Double2LongArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2LongArrayMap() : ((Double2LongArrayMap) current).clone()));
        register(Double2LongLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2LongLinkedOpenHashMap() : ((Double2LongLinkedOpenHashMap) current).clone()));
        register(Double2LongAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2LongAVLTreeMap() : ((Double2LongAVLTreeMap) current).clone()));
        register(Double2LongRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2LongRBTreeMap() : ((Double2LongRBTreeMap) current).clone()));
        register(Double2LongOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Double2LongOpenCustomHashMap) current).clone()));
        register(Double2LongSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2LongAVLTreeMap();
            var result = new Double2LongAVLTreeMap(((Double2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Double2LongSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2LongSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2LongAVLTreeMap();
            var result = new Double2LongAVLTreeMap(((Double2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Double2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2LongSortedMaps.unmodifiable((Double2LongSortedMap) decoded)));
        register(Double2LongSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2LongAVLTreeMap();
            var result = new Double2LongAVLTreeMap(((Double2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Double2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2LongSortedMaps.unmodifiable((Double2LongSortedMap) decoded)));
        register(Double2LongSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2LongAVLTreeMap();
            var result = new Double2LongAVLTreeMap(((Double2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Double2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2LongSortedMaps.unmodifiable((Double2LongSortedMap) decoded)));
        register(Double2LongSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2LongAVLTreeMap();
            var result = new Double2LongAVLTreeMap(((Double2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Double2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2LongSortedMaps.synchronize((Double2LongSortedMap) decoded)));
        register(Double2LongMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2LongMaps.unmodifiable((Double2LongMap) decoded)));
        register(Double2LongMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2LongMaps.unmodifiable((Double2LongMap) decoded)));
        register(Double2LongMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Double2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2LongMaps.unmodifiable((Double2LongMap) decoded)));
        register(Double2LongMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2LongMaps.synchronize((Double2LongMap) decoded)));
        register(Double2FloatMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2FloatMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2FloatOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2FloatOpenHashMap() : ((Double2FloatOpenHashMap) current).clone()));
        register(Double2FloatArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2FloatArrayMap() : ((Double2FloatArrayMap) current).clone()));
        register(Double2FloatLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2FloatLinkedOpenHashMap() : ((Double2FloatLinkedOpenHashMap) current).clone()));
        register(Double2FloatAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2FloatAVLTreeMap() : ((Double2FloatAVLTreeMap) current).clone()));
        register(Double2FloatRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2FloatRBTreeMap() : ((Double2FloatRBTreeMap) current).clone()));
        register(Double2FloatOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Double2FloatOpenCustomHashMap) current).clone()));
        register(Double2FloatSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2FloatAVLTreeMap();
            var result = new Double2FloatAVLTreeMap(((Double2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Double2FloatSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2FloatSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2FloatAVLTreeMap();
            var result = new Double2FloatAVLTreeMap(((Double2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Double2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2FloatSortedMaps.unmodifiable((Double2FloatSortedMap) decoded)));
        register(Double2FloatSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2FloatAVLTreeMap();
            var result = new Double2FloatAVLTreeMap(((Double2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Double2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2FloatSortedMaps.unmodifiable((Double2FloatSortedMap) decoded)));
        register(Double2FloatSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2FloatAVLTreeMap();
            var result = new Double2FloatAVLTreeMap(((Double2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Double2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2FloatSortedMaps.unmodifiable((Double2FloatSortedMap) decoded)));
        register(Double2FloatSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2FloatAVLTreeMap();
            var result = new Double2FloatAVLTreeMap(((Double2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Double2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2FloatSortedMaps.synchronize((Double2FloatSortedMap) decoded)));
        register(Double2FloatMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2FloatMaps.unmodifiable((Double2FloatMap) decoded)));
        register(Double2FloatMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2FloatMaps.unmodifiable((Double2FloatMap) decoded)));
        register(Double2FloatMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Double2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2FloatMaps.unmodifiable((Double2FloatMap) decoded)));
        register(Double2FloatMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2FloatMaps.synchronize((Double2FloatMap) decoded)));
        register(Double2DoubleMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2DoubleMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2DoubleOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2DoubleOpenHashMap() : ((Double2DoubleOpenHashMap) current).clone()));
        register(Double2DoubleArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2DoubleArrayMap() : ((Double2DoubleArrayMap) current).clone()));
        register(Double2DoubleLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2DoubleLinkedOpenHashMap() : ((Double2DoubleLinkedOpenHashMap) current).clone()));
        register(Double2DoubleAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2DoubleAVLTreeMap() : ((Double2DoubleAVLTreeMap) current).clone()));
        register(Double2DoubleRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2DoubleRBTreeMap() : ((Double2DoubleRBTreeMap) current).clone()));
        register(Double2DoubleOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Double2DoubleOpenCustomHashMap) current).clone()));
        register(Double2DoubleSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2DoubleAVLTreeMap();
            var result = new Double2DoubleAVLTreeMap(((Double2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Double2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2DoubleSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2DoubleAVLTreeMap();
            var result = new Double2DoubleAVLTreeMap(((Double2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Double2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2DoubleSortedMaps.unmodifiable((Double2DoubleSortedMap) decoded)));
        register(Double2DoubleSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2DoubleAVLTreeMap();
            var result = new Double2DoubleAVLTreeMap(((Double2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Double2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2DoubleSortedMaps.unmodifiable((Double2DoubleSortedMap) decoded)));
        register(Double2DoubleSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2DoubleAVLTreeMap();
            var result = new Double2DoubleAVLTreeMap(((Double2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Double2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2DoubleSortedMaps.unmodifiable((Double2DoubleSortedMap) decoded)));
        register(Double2DoubleSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2DoubleAVLTreeMap();
            var result = new Double2DoubleAVLTreeMap(((Double2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Double2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2DoubleSortedMaps.synchronize((Double2DoubleSortedMap) decoded)));
        register(Double2DoubleMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2DoubleMaps.unmodifiable((Double2DoubleMap) decoded)));
        register(Double2DoubleMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2DoubleMaps.unmodifiable((Double2DoubleMap) decoded)));
        register(Double2DoubleMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Double2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2DoubleMaps.unmodifiable((Double2DoubleMap) decoded)));
        register(Double2DoubleMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2DoubleMaps.synchronize((Double2DoubleMap) decoded)));
        register(Double2ObjectMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ObjectMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2ObjectOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ObjectOpenHashMap() : ((Double2ObjectOpenHashMap) current).clone()));
        register(Double2ObjectArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ObjectArrayMap() : ((Double2ObjectArrayMap) current).clone()));
        register(Double2ObjectLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2ObjectLinkedOpenHashMap() : ((Double2ObjectLinkedOpenHashMap) current).clone()));
        register(Double2ObjectAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ObjectAVLTreeMap() : ((Double2ObjectAVLTreeMap) current).clone()));
        register(Double2ObjectRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ObjectRBTreeMap() : ((Double2ObjectRBTreeMap) current).clone()));
        register(Double2ObjectOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Double2ObjectOpenCustomHashMap) current).clone()));
        register(Double2ObjectSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ObjectAVLTreeMap();
            var result = new Double2ObjectAVLTreeMap(((Double2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2ObjectSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ObjectAVLTreeMap();
            var result = new Double2ObjectAVLTreeMap(((Double2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ObjectSortedMaps.unmodifiable((Double2ObjectSortedMap) decoded)));
        register(Double2ObjectSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ObjectAVLTreeMap();
            var result = new Double2ObjectAVLTreeMap(((Double2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ObjectSortedMaps.unmodifiable((Double2ObjectSortedMap) decoded)));
        register(Double2ObjectSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ObjectAVLTreeMap();
            var result = new Double2ObjectAVLTreeMap(((Double2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ObjectSortedMaps.unmodifiable((Double2ObjectSortedMap) decoded)));
        register(Double2ObjectSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ObjectAVLTreeMap();
            var result = new Double2ObjectAVLTreeMap(((Double2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ObjectSortedMaps.synchronize((Double2ObjectSortedMap) decoded)));
        register(Double2ObjectMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ObjectMaps.unmodifiable((Double2ObjectMap) decoded)));
        register(Double2ObjectMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ObjectMaps.unmodifiable((Double2ObjectMap) decoded)));
        register(Double2ObjectMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Double2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ObjectMaps.unmodifiable((Double2ObjectMap) decoded)));
        register(Double2ObjectMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ObjectMaps.synchronize((Double2ObjectMap) decoded)));
        register(Double2ReferenceMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ReferenceMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2ReferenceOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2ReferenceOpenHashMap() : ((Double2ReferenceOpenHashMap) current).clone()));
        register(Double2ReferenceArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Double2ReferenceArrayMap() : ((Double2ReferenceArrayMap) current).clone()));
        register(Double2ReferenceLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2ReferenceLinkedOpenHashMap() : ((Double2ReferenceLinkedOpenHashMap) current).clone()));
        register(Double2ReferenceAVLTreeMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2ReferenceAVLTreeMap() : ((Double2ReferenceAVLTreeMap) current).clone()));
        register(Double2ReferenceRBTreeMap.class, (type, current) -> new Construction(() -> current == null ?
                new Double2ReferenceRBTreeMap() : ((Double2ReferenceRBTreeMap) current).clone()));
        register(Double2ReferenceOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Double2ReferenceOpenCustomHashMap) current).clone()));
        register(Double2ReferenceSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ReferenceAVLTreeMap();
            var result = new Double2ReferenceAVLTreeMap(((Double2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Double2ReferenceSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ReferenceAVLTreeMap();
            var result = new Double2ReferenceAVLTreeMap(((Double2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ReferenceSortedMaps.unmodifiable((Double2ReferenceSortedMap) decoded)));
        register(Double2ReferenceSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ReferenceAVLTreeMap();
            var result = new Double2ReferenceAVLTreeMap(((Double2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ReferenceSortedMaps.unmodifiable((Double2ReferenceSortedMap) decoded)));
        register(Double2ReferenceSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ReferenceAVLTreeMap();
            var result = new Double2ReferenceAVLTreeMap(((Double2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ReferenceSortedMaps.unmodifiable((Double2ReferenceSortedMap) decoded)));
        register(Double2ReferenceSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Double2ReferenceAVLTreeMap();
            var result = new Double2ReferenceAVLTreeMap(((Double2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Double2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ReferenceSortedMaps.synchronize((Double2ReferenceSortedMap) decoded)));
        register(Double2ReferenceMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ReferenceMaps.unmodifiable((Double2ReferenceMap) decoded)));
        register(Double2ReferenceMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ReferenceMaps.unmodifiable((Double2ReferenceMap) decoded)));
        register(Double2ReferenceMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Double2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ReferenceMaps.unmodifiable((Double2ReferenceMap) decoded)));
        register(Double2ReferenceMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Double2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Double2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Double2ReferenceMaps.synchronize((Double2ReferenceMap) decoded)));
        register(DoubleCollection.class, (type, current) -> new Construction(() -> new DoubleArrayList()));
        register(DoubleList.class, (type, current) -> new Construction(() -> new DoubleArrayList()));
        register(DoubleSet.class, (type, current) -> new Construction(() -> new DoubleOpenHashSet()));
        register(DoubleSortedSet.class, (type, current) -> new Construction(() -> current == null ?
                new DoubleRBTreeSet() : new DoubleRBTreeSet(((DoubleSortedSet) current).comparator())));
        register(DoubleBigList.class, (type, current) -> new Construction(() -> new DoubleBigArrayBigList()));
        register(DoubleArrayList.class, (type, current) -> new Construction(
                () -> current == null ? new DoubleArrayList() : ((DoubleArrayList) current).clone()));
        register(DoubleArraySet.class, (type, current) -> new Construction(
                () -> current == null ? new DoubleArraySet() : ((DoubleArraySet) current).clone()));
        register(DoubleOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new DoubleOpenHashSet() : ((DoubleOpenHashSet) current).clone()));
        register(DoubleLinkedOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new DoubleLinkedOpenHashSet() : ((DoubleLinkedOpenHashSet) current).clone()));
        register(DoubleAVLTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new DoubleAVLTreeSet() : ((DoubleAVLTreeSet) current).clone()));
        register(DoubleRBTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new DoubleRBTreeSet() : ((DoubleRBTreeSet) current).clone()));
        register(DoubleBigArrayBigList.class, (type, current) -> new Construction(
                () -> current == null ? new DoubleBigArrayBigList() : ((DoubleBigArrayBigList) current).clone()));
        register(DoubleOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((DoubleOpenCustomHashSet) current).clone()));
        register(DoubleLinkedOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((DoubleLinkedOpenCustomHashSet) current).clone()));
        register(DoubleCollections.unmodifiable(new DoubleArrayList()).getClass(),
                (type, current) -> new Construction(() -> new DoubleArrayList(),
                        decoded -> DoubleCollections.unmodifiable((DoubleCollection) decoded)));
        register(DoubleCollections.unmodifiable(new DoubleArrayList()).getClass(),
                (type, current) -> new Construction(() -> new DoubleArrayList(),
                        decoded -> DoubleCollections.unmodifiable((DoubleCollection) decoded)));
        register(DoubleCollections.synchronize(new DoubleArrayList()).getClass(),
                (type, current) -> new Construction(() -> new DoubleArrayList(),
                        decoded -> DoubleCollections.synchronize((DoubleCollection) decoded)));
        register(DoubleLists.UnmodifiableList.class, (type, current) -> new Construction(() -> new DoubleArrayList(),
                decoded -> DoubleLists.unmodifiable((DoubleList) decoded)));
        register(DoubleLists.EmptyList.class, (type, current) -> new Construction(() -> new DoubleArrayList(),
                decoded -> DoubleLists.unmodifiable((DoubleList) decoded)));
        register(DoubleLists.Singleton.class, (type, current) -> new Construction(() -> new DoubleArrayList(),
                decoded -> DoubleLists.unmodifiable((DoubleList) decoded)));
        register(DoubleLists.SynchronizedList.class, (type, current) -> new Construction(() -> new DoubleArrayList(),
                decoded -> DoubleLists.synchronize((DoubleList) decoded)));
        register(DoubleLists.UnmodifiableRandomAccessList.class,
                (type, current) -> new Construction(() -> new DoubleArrayList(),
                        decoded -> DoubleLists.unmodifiable((DoubleList) decoded)));
        register(DoubleLists.SynchronizedRandomAccessList.class,
                (type, current) -> new Construction(() -> new DoubleArrayList(),
                        decoded -> DoubleLists.synchronize((DoubleList) decoded)));
        register(DoubleSets.UnmodifiableSet.class, (type, current) -> new Construction(() -> new DoubleOpenHashSet(),
                decoded -> DoubleSets.unmodifiable((DoubleSet) decoded)));
        register(DoubleSets.EmptySet.class, (type, current) -> new Construction(() -> new DoubleOpenHashSet(),
                decoded -> DoubleSets.unmodifiable((DoubleSet) decoded)));
        register(DoubleSets.Singleton.class, (type, current) -> new Construction(() -> new DoubleOpenHashSet(),
                decoded -> DoubleSets.unmodifiable((DoubleSet) decoded)));
        register(DoubleSets.SynchronizedSet.class, (type, current) -> new Construction(() -> new DoubleOpenHashSet(),
                decoded -> DoubleSets.synchronize((DoubleSet) decoded)));
        register(DoubleSortedSets.UnmodifiableSortedSet.class,
                (type, current) -> new Construction(
                        () -> new DoubleRBTreeSet(current == null ? null : ((DoubleSortedSet) current).comparator()),
                        decoded -> DoubleSortedSets.unmodifiable((DoubleSortedSet) decoded)));
        register(DoubleSortedSets.EmptySet.class,
                (type, current) -> new Construction(
                        () -> new DoubleRBTreeSet(current == null ? null : ((DoubleSortedSet) current).comparator()),
                        decoded -> DoubleSortedSets.unmodifiable((DoubleSortedSet) decoded)));
        register(DoubleSortedSets.Singleton.class,
                (type, current) -> new Construction(
                        () -> new DoubleRBTreeSet(current == null ? null : ((DoubleSortedSet) current).comparator()),
                        decoded -> DoubleSortedSets.unmodifiable((DoubleSortedSet) decoded)));
        register(DoubleSortedSets.SynchronizedSortedSet.class,
                (type, current) -> new Construction(
                        () -> new DoubleRBTreeSet(current == null ? null : ((DoubleSortedSet) current).comparator()),
                        decoded -> DoubleSortedSets.synchronize((DoubleSortedSet) decoded)));
        register(DoubleBigLists.UnmodifiableBigList.class,
                (type, current) -> new Construction(() -> new DoubleBigArrayBigList(),
                        decoded -> DoubleBigLists.unmodifiable((DoubleBigList) decoded)));
        register(DoubleBigLists.EmptyBigList.class,
                (type, current) -> new Construction(() -> new DoubleBigArrayBigList(),
                        decoded -> DoubleBigLists.unmodifiable((DoubleBigList) decoded)));
        register(DoubleBigLists.Singleton.class, (type, current) -> new Construction(() -> new DoubleBigArrayBigList(),
                decoded -> DoubleBigLists.unmodifiable((DoubleBigList) decoded)));
        register(DoubleBigLists.SynchronizedBigList.class,
                (type, current) -> new Construction(() -> new DoubleBigArrayBigList(),
                        decoded -> DoubleBigLists.synchronize((DoubleBigList) decoded)));
        register(DoubleImmutableList.class, (type, current) -> new Construction(() -> new DoubleArrayList(),
                decoded -> new DoubleImmutableList((Collection) decoded)));
        register(new DoubleImmutableList(new double[] { 0, 1 }).subList(0, 1).getClass(),
                (type, current) -> new Construction(() -> new DoubleArrayList(),
                        decoded -> new DoubleImmutableList((Collection) decoded)));
        register(DoubleMappedBigList.class, (type, current) -> new Construction(() -> new DoubleBigArrayBigList(),
                decoded -> updateMapped((BigList) current, (BigList) decoded)));
    }

    private static void registerObject() {
        register(Object2BooleanMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2BooleanMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2BooleanOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2BooleanOpenHashMap() : ((Object2BooleanOpenHashMap) current).clone()));
        register(Object2BooleanArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2BooleanArrayMap() : ((Object2BooleanArrayMap) current).clone()));
        register(Object2BooleanLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2BooleanLinkedOpenHashMap() : ((Object2BooleanLinkedOpenHashMap) current).clone()));
        register(Object2BooleanAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2BooleanAVLTreeMap() : ((Object2BooleanAVLTreeMap) current).clone()));
        register(Object2BooleanRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2BooleanRBTreeMap() : ((Object2BooleanRBTreeMap) current).clone()));
        register(Object2BooleanOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2BooleanOpenCustomHashMap) current).clone()));
        register(Object2BooleanLinkedOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2BooleanLinkedOpenCustomHashMap) current).clone()));
        register(Object2BooleanSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2BooleanAVLTreeMap();
            var result = new Object2BooleanAVLTreeMap(((Object2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Object2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2BooleanSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2BooleanAVLTreeMap();
            var result = new Object2BooleanAVLTreeMap(((Object2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Object2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2BooleanSortedMaps.unmodifiable((Object2BooleanSortedMap) decoded)));
        register(Object2BooleanSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2BooleanAVLTreeMap();
            var result = new Object2BooleanAVLTreeMap(((Object2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Object2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2BooleanSortedMaps.unmodifiable((Object2BooleanSortedMap) decoded)));
        register(Object2BooleanSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2BooleanAVLTreeMap();
            var result = new Object2BooleanAVLTreeMap(((Object2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Object2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2BooleanSortedMaps.unmodifiable((Object2BooleanSortedMap) decoded)));
        register(Object2BooleanSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2BooleanAVLTreeMap();
            var result = new Object2BooleanAVLTreeMap(((Object2BooleanSortedMap) current).comparator());
            result.defaultReturnValue(((Object2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2BooleanSortedMaps.synchronize((Object2BooleanSortedMap) decoded)));
        register(Object2BooleanMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2BooleanMaps.unmodifiable((Object2BooleanMap) decoded)));
        register(Object2BooleanMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2BooleanMaps.unmodifiable((Object2BooleanMap) decoded)));
        register(Object2BooleanMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Object2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2BooleanMaps.unmodifiable((Object2BooleanMap) decoded)));
        register(Object2BooleanMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2BooleanMaps.synchronize((Object2BooleanMap) decoded)));
        register(Object2ByteMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ByteMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2ByteOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ByteOpenHashMap() : ((Object2ByteOpenHashMap) current).clone()));
        register(Object2ByteArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ByteArrayMap() : ((Object2ByteArrayMap) current).clone()));
        register(Object2ByteLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2ByteLinkedOpenHashMap() : ((Object2ByteLinkedOpenHashMap) current).clone()));
        register(Object2ByteAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ByteAVLTreeMap() : ((Object2ByteAVLTreeMap) current).clone()));
        register(Object2ByteRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ByteRBTreeMap() : ((Object2ByteRBTreeMap) current).clone()));
        register(Object2ByteOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2ByteOpenCustomHashMap) current).clone()));
        register(Object2ByteLinkedOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2ByteLinkedOpenCustomHashMap) current).clone()));
        register(Object2ByteSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ByteAVLTreeMap();
            var result = new Object2ByteAVLTreeMap(((Object2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ByteSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2ByteSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ByteAVLTreeMap();
            var result = new Object2ByteAVLTreeMap(((Object2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ByteSortedMaps.unmodifiable((Object2ByteSortedMap) decoded)));
        register(Object2ByteSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ByteAVLTreeMap();
            var result = new Object2ByteAVLTreeMap(((Object2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ByteSortedMaps.unmodifiable((Object2ByteSortedMap) decoded)));
        register(Object2ByteSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ByteAVLTreeMap();
            var result = new Object2ByteAVLTreeMap(((Object2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ByteSortedMaps.unmodifiable((Object2ByteSortedMap) decoded)));
        register(Object2ByteSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ByteAVLTreeMap();
            var result = new Object2ByteAVLTreeMap(((Object2ByteSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ByteSortedMaps.synchronize((Object2ByteSortedMap) decoded)));
        register(Object2ByteMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ByteMaps.unmodifiable((Object2ByteMap) decoded)));
        register(Object2ByteMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ByteMaps.unmodifiable((Object2ByteMap) decoded)));
        register(Object2ByteMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Object2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ByteMaps.unmodifiable((Object2ByteMap) decoded)));
        register(Object2ByteMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ByteMaps.synchronize((Object2ByteMap) decoded)));
        register(Object2ShortMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ShortMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2ShortOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ShortOpenHashMap() : ((Object2ShortOpenHashMap) current).clone()));
        register(Object2ShortArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ShortArrayMap() : ((Object2ShortArrayMap) current).clone()));
        register(Object2ShortLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2ShortLinkedOpenHashMap() : ((Object2ShortLinkedOpenHashMap) current).clone()));
        register(Object2ShortAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ShortAVLTreeMap() : ((Object2ShortAVLTreeMap) current).clone()));
        register(Object2ShortRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ShortRBTreeMap() : ((Object2ShortRBTreeMap) current).clone()));
        register(Object2ShortOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2ShortOpenCustomHashMap) current).clone()));
        register(Object2ShortLinkedOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2ShortLinkedOpenCustomHashMap) current).clone()));
        register(Object2ShortSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ShortAVLTreeMap();
            var result = new Object2ShortAVLTreeMap(((Object2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ShortSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2ShortSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ShortAVLTreeMap();
            var result = new Object2ShortAVLTreeMap(((Object2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ShortSortedMaps.unmodifiable((Object2ShortSortedMap) decoded)));
        register(Object2ShortSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ShortAVLTreeMap();
            var result = new Object2ShortAVLTreeMap(((Object2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ShortSortedMaps.unmodifiable((Object2ShortSortedMap) decoded)));
        register(Object2ShortSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ShortAVLTreeMap();
            var result = new Object2ShortAVLTreeMap(((Object2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ShortSortedMaps.unmodifiable((Object2ShortSortedMap) decoded)));
        register(Object2ShortSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ShortAVLTreeMap();
            var result = new Object2ShortAVLTreeMap(((Object2ShortSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ShortSortedMaps.synchronize((Object2ShortSortedMap) decoded)));
        register(Object2ShortMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ShortMaps.unmodifiable((Object2ShortMap) decoded)));
        register(Object2ShortMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ShortMaps.unmodifiable((Object2ShortMap) decoded)));
        register(Object2ShortMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Object2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ShortMaps.unmodifiable((Object2ShortMap) decoded)));
        register(Object2ShortMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ShortMaps.synchronize((Object2ShortMap) decoded)));
        register(Object2CharMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2CharMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2CharOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2CharOpenHashMap() : ((Object2CharOpenHashMap) current).clone()));
        register(Object2CharArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2CharArrayMap() : ((Object2CharArrayMap) current).clone()));
        register(Object2CharLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2CharLinkedOpenHashMap() : ((Object2CharLinkedOpenHashMap) current).clone()));
        register(Object2CharAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2CharAVLTreeMap() : ((Object2CharAVLTreeMap) current).clone()));
        register(Object2CharRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2CharRBTreeMap() : ((Object2CharRBTreeMap) current).clone()));
        register(Object2CharOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2CharOpenCustomHashMap) current).clone()));
        register(Object2CharLinkedOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2CharLinkedOpenCustomHashMap) current).clone()));
        register(Object2CharSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2CharAVLTreeMap();
            var result = new Object2CharAVLTreeMap(((Object2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Object2CharSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2CharSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2CharAVLTreeMap();
            var result = new Object2CharAVLTreeMap(((Object2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Object2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2CharSortedMaps.unmodifiable((Object2CharSortedMap) decoded)));
        register(Object2CharSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2CharAVLTreeMap();
            var result = new Object2CharAVLTreeMap(((Object2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Object2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2CharSortedMaps.unmodifiable((Object2CharSortedMap) decoded)));
        register(Object2CharSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2CharAVLTreeMap();
            var result = new Object2CharAVLTreeMap(((Object2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Object2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2CharSortedMaps.unmodifiable((Object2CharSortedMap) decoded)));
        register(Object2CharSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2CharAVLTreeMap();
            var result = new Object2CharAVLTreeMap(((Object2CharSortedMap) current).comparator());
            result.defaultReturnValue(((Object2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2CharSortedMaps.synchronize((Object2CharSortedMap) decoded)));
        register(Object2CharMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2CharMaps.unmodifiable((Object2CharMap) decoded)));
        register(Object2CharMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2CharMaps.unmodifiable((Object2CharMap) decoded)));
        register(Object2CharMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Object2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2CharMaps.unmodifiable((Object2CharMap) decoded)));
        register(Object2CharMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2CharMaps.synchronize((Object2CharMap) decoded)));
        register(Object2IntMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2IntMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2IntOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2IntOpenHashMap() : ((Object2IntOpenHashMap) current).clone()));
        register(Object2IntArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2IntArrayMap() : ((Object2IntArrayMap) current).clone()));
        register(Object2IntLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2IntLinkedOpenHashMap() : ((Object2IntLinkedOpenHashMap) current).clone()));
        register(Object2IntAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2IntAVLTreeMap() : ((Object2IntAVLTreeMap) current).clone()));
        register(Object2IntRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2IntRBTreeMap() : ((Object2IntRBTreeMap) current).clone()));
        register(Object2IntOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2IntOpenCustomHashMap) current).clone()));
        register(Object2IntLinkedOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2IntLinkedOpenCustomHashMap) current).clone()));
        register(Object2IntSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2IntAVLTreeMap();
            var result = new Object2IntAVLTreeMap(((Object2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Object2IntSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2IntSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2IntAVLTreeMap();
            var result = new Object2IntAVLTreeMap(((Object2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Object2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2IntSortedMaps.unmodifiable((Object2IntSortedMap) decoded)));
        register(Object2IntSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2IntAVLTreeMap();
            var result = new Object2IntAVLTreeMap(((Object2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Object2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2IntSortedMaps.unmodifiable((Object2IntSortedMap) decoded)));
        register(Object2IntSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2IntAVLTreeMap();
            var result = new Object2IntAVLTreeMap(((Object2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Object2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2IntSortedMaps.unmodifiable((Object2IntSortedMap) decoded)));
        register(Object2IntSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2IntAVLTreeMap();
            var result = new Object2IntAVLTreeMap(((Object2IntSortedMap) current).comparator());
            result.defaultReturnValue(((Object2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2IntSortedMaps.synchronize((Object2IntSortedMap) decoded)));
        register(Object2IntMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2IntMaps.unmodifiable((Object2IntMap) decoded)));
        register(Object2IntMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2IntMaps.unmodifiable((Object2IntMap) decoded)));
        register(Object2IntMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Object2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2IntMaps.unmodifiable((Object2IntMap) decoded)));
        register(Object2IntMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2IntMaps.synchronize((Object2IntMap) decoded)));
        register(Object2LongMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2LongMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2LongOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2LongOpenHashMap() : ((Object2LongOpenHashMap) current).clone()));
        register(Object2LongArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2LongArrayMap() : ((Object2LongArrayMap) current).clone()));
        register(Object2LongLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2LongLinkedOpenHashMap() : ((Object2LongLinkedOpenHashMap) current).clone()));
        register(Object2LongAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2LongAVLTreeMap() : ((Object2LongAVLTreeMap) current).clone()));
        register(Object2LongRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2LongRBTreeMap() : ((Object2LongRBTreeMap) current).clone()));
        register(Object2LongOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2LongOpenCustomHashMap) current).clone()));
        register(Object2LongLinkedOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2LongLinkedOpenCustomHashMap) current).clone()));
        register(Object2LongSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2LongAVLTreeMap();
            var result = new Object2LongAVLTreeMap(((Object2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Object2LongSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2LongSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2LongAVLTreeMap();
            var result = new Object2LongAVLTreeMap(((Object2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Object2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2LongSortedMaps.unmodifiable((Object2LongSortedMap) decoded)));
        register(Object2LongSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2LongAVLTreeMap();
            var result = new Object2LongAVLTreeMap(((Object2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Object2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2LongSortedMaps.unmodifiable((Object2LongSortedMap) decoded)));
        register(Object2LongSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2LongAVLTreeMap();
            var result = new Object2LongAVLTreeMap(((Object2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Object2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2LongSortedMaps.unmodifiable((Object2LongSortedMap) decoded)));
        register(Object2LongSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2LongAVLTreeMap();
            var result = new Object2LongAVLTreeMap(((Object2LongSortedMap) current).comparator());
            result.defaultReturnValue(((Object2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2LongSortedMaps.synchronize((Object2LongSortedMap) decoded)));
        register(Object2LongMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2LongMaps.unmodifiable((Object2LongMap) decoded)));
        register(Object2LongMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2LongMaps.unmodifiable((Object2LongMap) decoded)));
        register(Object2LongMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Object2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2LongMaps.unmodifiable((Object2LongMap) decoded)));
        register(Object2LongMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2LongMaps.synchronize((Object2LongMap) decoded)));
        register(Object2FloatMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2FloatMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2FloatOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2FloatOpenHashMap() : ((Object2FloatOpenHashMap) current).clone()));
        register(Object2FloatArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2FloatArrayMap() : ((Object2FloatArrayMap) current).clone()));
        register(Object2FloatLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2FloatLinkedOpenHashMap() : ((Object2FloatLinkedOpenHashMap) current).clone()));
        register(Object2FloatAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2FloatAVLTreeMap() : ((Object2FloatAVLTreeMap) current).clone()));
        register(Object2FloatRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2FloatRBTreeMap() : ((Object2FloatRBTreeMap) current).clone()));
        register(Object2FloatOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2FloatOpenCustomHashMap) current).clone()));
        register(Object2FloatLinkedOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2FloatLinkedOpenCustomHashMap) current).clone()));
        register(Object2FloatSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2FloatAVLTreeMap();
            var result = new Object2FloatAVLTreeMap(((Object2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Object2FloatSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2FloatSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2FloatAVLTreeMap();
            var result = new Object2FloatAVLTreeMap(((Object2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Object2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2FloatSortedMaps.unmodifiable((Object2FloatSortedMap) decoded)));
        register(Object2FloatSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2FloatAVLTreeMap();
            var result = new Object2FloatAVLTreeMap(((Object2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Object2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2FloatSortedMaps.unmodifiable((Object2FloatSortedMap) decoded)));
        register(Object2FloatSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2FloatAVLTreeMap();
            var result = new Object2FloatAVLTreeMap(((Object2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Object2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2FloatSortedMaps.unmodifiable((Object2FloatSortedMap) decoded)));
        register(Object2FloatSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2FloatAVLTreeMap();
            var result = new Object2FloatAVLTreeMap(((Object2FloatSortedMap) current).comparator());
            result.defaultReturnValue(((Object2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2FloatSortedMaps.synchronize((Object2FloatSortedMap) decoded)));
        register(Object2FloatMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2FloatMaps.unmodifiable((Object2FloatMap) decoded)));
        register(Object2FloatMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2FloatMaps.unmodifiable((Object2FloatMap) decoded)));
        register(Object2FloatMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Object2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2FloatMaps.unmodifiable((Object2FloatMap) decoded)));
        register(Object2FloatMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2FloatMaps.synchronize((Object2FloatMap) decoded)));
        register(Object2DoubleMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2DoubleMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2DoubleOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2DoubleOpenHashMap() : ((Object2DoubleOpenHashMap) current).clone()));
        register(Object2DoubleArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2DoubleArrayMap() : ((Object2DoubleArrayMap) current).clone()));
        register(Object2DoubleLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2DoubleLinkedOpenHashMap() : ((Object2DoubleLinkedOpenHashMap) current).clone()));
        register(Object2DoubleAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2DoubleAVLTreeMap() : ((Object2DoubleAVLTreeMap) current).clone()));
        register(Object2DoubleRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2DoubleRBTreeMap() : ((Object2DoubleRBTreeMap) current).clone()));
        register(Object2DoubleOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2DoubleOpenCustomHashMap) current).clone()));
        register(Object2DoubleLinkedOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2DoubleLinkedOpenCustomHashMap) current).clone()));
        register(Object2DoubleSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2DoubleAVLTreeMap();
            var result = new Object2DoubleAVLTreeMap(((Object2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Object2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2DoubleSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2DoubleAVLTreeMap();
            var result = new Object2DoubleAVLTreeMap(((Object2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Object2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2DoubleSortedMaps.unmodifiable((Object2DoubleSortedMap) decoded)));
        register(Object2DoubleSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2DoubleAVLTreeMap();
            var result = new Object2DoubleAVLTreeMap(((Object2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Object2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2DoubleSortedMaps.unmodifiable((Object2DoubleSortedMap) decoded)));
        register(Object2DoubleSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2DoubleAVLTreeMap();
            var result = new Object2DoubleAVLTreeMap(((Object2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Object2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2DoubleSortedMaps.unmodifiable((Object2DoubleSortedMap) decoded)));
        register(Object2DoubleSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2DoubleAVLTreeMap();
            var result = new Object2DoubleAVLTreeMap(((Object2DoubleSortedMap) current).comparator());
            result.defaultReturnValue(((Object2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2DoubleSortedMaps.synchronize((Object2DoubleSortedMap) decoded)));
        register(Object2DoubleMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2DoubleMaps.unmodifiable((Object2DoubleMap) decoded)));
        register(Object2DoubleMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2DoubleMaps.unmodifiable((Object2DoubleMap) decoded)));
        register(Object2DoubleMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Object2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2DoubleMaps.unmodifiable((Object2DoubleMap) decoded)));
        register(Object2DoubleMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2DoubleMaps.synchronize((Object2DoubleMap) decoded)));
        register(Object2ObjectMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ObjectMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2ObjectOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ObjectOpenHashMap() : ((Object2ObjectOpenHashMap) current).clone()));
        register(Object2ObjectArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ObjectArrayMap() : ((Object2ObjectArrayMap) current).clone()));
        register(Object2ObjectLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2ObjectLinkedOpenHashMap() : ((Object2ObjectLinkedOpenHashMap) current).clone()));
        register(Object2ObjectAVLTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ObjectAVLTreeMap() : ((Object2ObjectAVLTreeMap) current).clone()));
        register(Object2ObjectRBTreeMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ObjectRBTreeMap() : ((Object2ObjectRBTreeMap) current).clone()));
        register(Object2ObjectOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2ObjectOpenCustomHashMap) current).clone()));
        register(Object2ObjectLinkedOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2ObjectLinkedOpenCustomHashMap) current).clone()));
        register(Object2ObjectSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ObjectAVLTreeMap();
            var result = new Object2ObjectAVLTreeMap(((Object2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2ObjectSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ObjectAVLTreeMap();
            var result = new Object2ObjectAVLTreeMap(((Object2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ObjectSortedMaps.unmodifiable((Object2ObjectSortedMap) decoded)));
        register(Object2ObjectSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ObjectAVLTreeMap();
            var result = new Object2ObjectAVLTreeMap(((Object2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ObjectSortedMaps.unmodifiable((Object2ObjectSortedMap) decoded)));
        register(Object2ObjectSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ObjectAVLTreeMap();
            var result = new Object2ObjectAVLTreeMap(((Object2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ObjectSortedMaps.unmodifiable((Object2ObjectSortedMap) decoded)));
        register(Object2ObjectSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ObjectAVLTreeMap();
            var result = new Object2ObjectAVLTreeMap(((Object2ObjectSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ObjectSortedMaps.synchronize((Object2ObjectSortedMap) decoded)));
        register(Object2ObjectMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ObjectMaps.unmodifiable((Object2ObjectMap) decoded)));
        register(Object2ObjectMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ObjectMaps.unmodifiable((Object2ObjectMap) decoded)));
        register(Object2ObjectMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Object2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ObjectMaps.unmodifiable((Object2ObjectMap) decoded)));
        register(Object2ObjectMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ObjectMaps.synchronize((Object2ObjectMap) decoded)));
        register(Object2ReferenceMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ReferenceMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2ReferenceOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2ReferenceOpenHashMap() : ((Object2ReferenceOpenHashMap) current).clone()));
        register(Object2ReferenceArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Object2ReferenceArrayMap() : ((Object2ReferenceArrayMap) current).clone()));
        register(Object2ReferenceLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2ReferenceLinkedOpenHashMap() : ((Object2ReferenceLinkedOpenHashMap) current).clone()));
        register(Object2ReferenceAVLTreeMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2ReferenceAVLTreeMap() : ((Object2ReferenceAVLTreeMap) current).clone()));
        register(Object2ReferenceRBTreeMap.class, (type, current) -> new Construction(() -> current == null ?
                new Object2ReferenceRBTreeMap() : ((Object2ReferenceRBTreeMap) current).clone()));
        register(Object2ReferenceOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2ReferenceOpenCustomHashMap) current).clone()));
        register(Object2ReferenceLinkedOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Object2ReferenceLinkedOpenCustomHashMap) current).clone()));
        register(Object2ReferenceSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ReferenceAVLTreeMap();
            var result = new Object2ReferenceAVLTreeMap(((Object2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Object2ReferenceSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ReferenceAVLTreeMap();
            var result = new Object2ReferenceAVLTreeMap(((Object2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ReferenceSortedMaps.unmodifiable((Object2ReferenceSortedMap) decoded)));
        register(Object2ReferenceSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ReferenceAVLTreeMap();
            var result = new Object2ReferenceAVLTreeMap(((Object2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ReferenceSortedMaps.unmodifiable((Object2ReferenceSortedMap) decoded)));
        register(Object2ReferenceSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ReferenceAVLTreeMap();
            var result = new Object2ReferenceAVLTreeMap(((Object2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ReferenceSortedMaps.unmodifiable((Object2ReferenceSortedMap) decoded)));
        register(Object2ReferenceSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            if (current == null) return new Object2ReferenceAVLTreeMap();
            var result = new Object2ReferenceAVLTreeMap(((Object2ReferenceSortedMap) current).comparator());
            result.defaultReturnValue(((Object2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ReferenceSortedMaps.synchronize((Object2ReferenceSortedMap) decoded)));
        register(Object2ReferenceMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ReferenceMaps.unmodifiable((Object2ReferenceMap) decoded)));
        register(Object2ReferenceMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ReferenceMaps.unmodifiable((Object2ReferenceMap) decoded)));
        register(Object2ReferenceMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Object2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ReferenceMaps.unmodifiable((Object2ReferenceMap) decoded)));
        register(Object2ReferenceMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Object2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Object2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Object2ReferenceMaps.synchronize((Object2ReferenceMap) decoded)));
        register(ObjectCollection.class, (type, current) -> new Construction(() -> new ObjectArrayList()));
        register(ObjectList.class, (type, current) -> new Construction(() -> new ObjectArrayList()));
        register(ObjectSet.class, (type, current) -> new Construction(() -> new ObjectOpenHashSet()));
        register(ObjectSortedSet.class, (type, current) -> new Construction(() -> current == null ?
                new ObjectRBTreeSet() : new ObjectRBTreeSet(((ObjectSortedSet) current).comparator())));
        register(ObjectBigList.class, (type, current) -> new Construction(() -> new ObjectBigArrayBigList()));
        register(ObjectArrayList.class, (type, current) -> new Construction(
                () -> current == null ? new ObjectArrayList() : ((ObjectArrayList) current).clone()));
        register(ObjectArraySet.class, (type, current) -> new Construction(
                () -> current == null ? new ObjectArraySet() : ((ObjectArraySet) current).clone()));
        register(ObjectOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new ObjectOpenHashSet() : ((ObjectOpenHashSet) current).clone()));
        register(ObjectLinkedOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new ObjectLinkedOpenHashSet() : ((ObjectLinkedOpenHashSet) current).clone()));
        register(ObjectAVLTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new ObjectAVLTreeSet() : ((ObjectAVLTreeSet) current).clone()));
        register(ObjectRBTreeSet.class, (type, current) -> new Construction(
                () -> current == null ? new ObjectRBTreeSet() : ((ObjectRBTreeSet) current).clone()));
        register(ObjectBigArrayBigList.class, (type, current) -> new Construction(
                () -> current == null ? new ObjectBigArrayBigList() : ((ObjectBigArrayBigList) current).clone()));
        register(ObjectOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((ObjectOpenCustomHashSet) current).clone()));
        register(ObjectLinkedOpenCustomHashSet.class,
                (type, current) -> new Construction(() -> ((ObjectLinkedOpenCustomHashSet) current).clone()));
        register(ObjectCollections.unmodifiable(new ObjectArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ObjectArrayList(),
                        decoded -> ObjectCollections.unmodifiable((ObjectCollection) decoded)));
        register(ObjectCollections.unmodifiable(new ObjectArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ObjectArrayList(),
                        decoded -> ObjectCollections.unmodifiable((ObjectCollection) decoded)));
        register(ObjectCollections.synchronize(new ObjectArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ObjectArrayList(),
                        decoded -> ObjectCollections.synchronize((ObjectCollection) decoded)));
        register(ObjectLists.UnmodifiableList.class, (type, current) -> new Construction(() -> new ObjectArrayList(),
                decoded -> ObjectLists.unmodifiable((ObjectList) decoded)));
        register(ObjectLists.EmptyList.class, (type, current) -> new Construction(() -> new ObjectArrayList(),
                decoded -> ObjectLists.unmodifiable((ObjectList) decoded)));
        register(ObjectLists.Singleton.class, (type, current) -> new Construction(() -> new ObjectArrayList(),
                decoded -> ObjectLists.unmodifiable((ObjectList) decoded)));
        register(ObjectLists.SynchronizedList.class, (type, current) -> new Construction(() -> new ObjectArrayList(),
                decoded -> ObjectLists.synchronize((ObjectList) decoded)));
        register(ObjectLists.UnmodifiableRandomAccessList.class,
                (type, current) -> new Construction(() -> new ObjectArrayList(),
                        decoded -> ObjectLists.unmodifiable((ObjectList) decoded)));
        register(ObjectLists.SynchronizedRandomAccessList.class,
                (type, current) -> new Construction(() -> new ObjectArrayList(),
                        decoded -> ObjectLists.synchronize((ObjectList) decoded)));
        register(ObjectSets.UnmodifiableSet.class, (type, current) -> new Construction(() -> new ObjectOpenHashSet(),
                decoded -> ObjectSets.unmodifiable((ObjectSet) decoded)));
        register(ObjectSets.EmptySet.class, (type, current) -> new Construction(() -> new ObjectOpenHashSet(),
                decoded -> ObjectSets.unmodifiable((ObjectSet) decoded)));
        register(ObjectSets.Singleton.class, (type, current) -> new Construction(() -> new ObjectOpenHashSet(),
                decoded -> ObjectSets.unmodifiable((ObjectSet) decoded)));
        register(ObjectSets.SynchronizedSet.class, (type, current) -> new Construction(() -> new ObjectOpenHashSet(),
                decoded -> ObjectSets.synchronize((ObjectSet) decoded)));
        register(ObjectSortedSets.UnmodifiableSortedSet.class,
                (type, current) -> new Construction(
                        () -> new ObjectRBTreeSet(current == null ? null : ((ObjectSortedSet) current).comparator()),
                        decoded -> ObjectSortedSets.unmodifiable((ObjectSortedSet) decoded)));
        register(ObjectSortedSets.EmptySet.class,
                (type, current) -> new Construction(
                        () -> new ObjectRBTreeSet(current == null ? null : ((ObjectSortedSet) current).comparator()),
                        decoded -> ObjectSortedSets.unmodifiable((ObjectSortedSet) decoded)));
        register(ObjectSortedSets.Singleton.class,
                (type, current) -> new Construction(
                        () -> new ObjectRBTreeSet(current == null ? null : ((ObjectSortedSet) current).comparator()),
                        decoded -> ObjectSortedSets.unmodifiable((ObjectSortedSet) decoded)));
        register(ObjectSortedSets.SynchronizedSortedSet.class,
                (type, current) -> new Construction(
                        () -> new ObjectRBTreeSet(current == null ? null : ((ObjectSortedSet) current).comparator()),
                        decoded -> ObjectSortedSets.synchronize((ObjectSortedSet) decoded)));
        register(ObjectBigLists.UnmodifiableBigList.class,
                (type, current) -> new Construction(() -> new ObjectBigArrayBigList(),
                        decoded -> ObjectBigLists.unmodifiable((ObjectBigList) decoded)));
        register(ObjectBigLists.EmptyBigList.class,
                (type, current) -> new Construction(() -> new ObjectBigArrayBigList(),
                        decoded -> ObjectBigLists.unmodifiable((ObjectBigList) decoded)));
        register(ObjectBigLists.Singleton.class, (type, current) -> new Construction(() -> new ObjectBigArrayBigList(),
                decoded -> ObjectBigLists.unmodifiable((ObjectBigList) decoded)));
        register(ObjectBigLists.SynchronizedBigList.class,
                (type, current) -> new Construction(() -> new ObjectBigArrayBigList(),
                        decoded -> ObjectBigLists.synchronize((ObjectBigList) decoded)));
        register(ObjectImmutableList.class, (type, current) -> new Construction(() -> new ObjectArrayList(),
                decoded -> new ObjectImmutableList((Collection) decoded)));
        register(new ObjectImmutableList(new Object[] { null, null }).subList(0, 1).getClass(),
                (type, current) -> new Construction(() -> new ObjectArrayList(),
                        decoded -> new ObjectImmutableList((Collection) decoded)));
    }

    private static void registerReference() {
        register(Reference2BooleanMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2BooleanMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2BooleanOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2BooleanOpenHashMap() : ((Reference2BooleanOpenHashMap) current).clone()));
        register(Reference2BooleanArrayMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2BooleanArrayMap() : ((Reference2BooleanArrayMap) current).clone()));
        register(Reference2BooleanLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2BooleanLinkedOpenHashMap() : ((Reference2BooleanLinkedOpenHashMap) current).clone()));
        register(Reference2BooleanOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Reference2BooleanOpenCustomHashMap) current).clone()));
        register(Reference2BooleanSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2BooleanLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2BooleanSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2BooleanLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2BooleanSortedMaps.unmodifiable((Reference2BooleanSortedMap) decoded)));
        register(Reference2BooleanSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2BooleanLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2BooleanSortedMaps.unmodifiable((Reference2BooleanSortedMap) decoded)));
        register(Reference2BooleanSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2BooleanLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2BooleanSortedMaps.unmodifiable((Reference2BooleanSortedMap) decoded)));
        register(Reference2BooleanSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2BooleanLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2BooleanSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2BooleanSortedMaps.synchronize((Reference2BooleanSortedMap) decoded)));
        register(Reference2BooleanMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2BooleanMaps.unmodifiable((Reference2BooleanMap) decoded)));
        register(Reference2BooleanMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2BooleanMaps.unmodifiable((Reference2BooleanMap) decoded)));
        register(Reference2BooleanMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2BooleanMaps.unmodifiable((Reference2BooleanMap) decoded)));
        register(Reference2BooleanMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2BooleanOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2BooleanMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2BooleanMaps.synchronize((Reference2BooleanMap) decoded)));
        register(Reference2ByteMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ByteMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2ByteOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2ByteOpenHashMap() : ((Reference2ByteOpenHashMap) current).clone()));
        register(Reference2ByteArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Reference2ByteArrayMap() : ((Reference2ByteArrayMap) current).clone()));
        register(Reference2ByteLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2ByteLinkedOpenHashMap() : ((Reference2ByteLinkedOpenHashMap) current).clone()));
        register(Reference2ByteOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Reference2ByteOpenCustomHashMap) current).clone()));
        register(Reference2ByteSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ByteLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ByteSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2ByteSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ByteLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ByteSortedMaps.unmodifiable((Reference2ByteSortedMap) decoded)));
        register(Reference2ByteSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ByteLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ByteSortedMaps.unmodifiable((Reference2ByteSortedMap) decoded)));
        register(Reference2ByteSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ByteLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ByteSortedMaps.unmodifiable((Reference2ByteSortedMap) decoded)));
        register(Reference2ByteSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ByteLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ByteSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ByteSortedMaps.synchronize((Reference2ByteSortedMap) decoded)));
        register(Reference2ByteMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ByteMaps.unmodifiable((Reference2ByteMap) decoded)));
        register(Reference2ByteMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ByteMaps.unmodifiable((Reference2ByteMap) decoded)));
        register(Reference2ByteMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ByteMaps.unmodifiable((Reference2ByteMap) decoded)));
        register(Reference2ByteMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ByteOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ByteMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ByteMaps.synchronize((Reference2ByteMap) decoded)));
        register(Reference2ShortMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ShortMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2ShortOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2ShortOpenHashMap() : ((Reference2ShortOpenHashMap) current).clone()));
        register(Reference2ShortArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Reference2ShortArrayMap() : ((Reference2ShortArrayMap) current).clone()));
        register(Reference2ShortLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2ShortLinkedOpenHashMap() : ((Reference2ShortLinkedOpenHashMap) current).clone()));
        register(Reference2ShortOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Reference2ShortOpenCustomHashMap) current).clone()));
        register(Reference2ShortSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ShortLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ShortSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2ShortSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ShortLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ShortSortedMaps.unmodifiable((Reference2ShortSortedMap) decoded)));
        register(Reference2ShortSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ShortLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ShortSortedMaps.unmodifiable((Reference2ShortSortedMap) decoded)));
        register(Reference2ShortSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ShortLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ShortSortedMaps.unmodifiable((Reference2ShortSortedMap) decoded)));
        register(Reference2ShortSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ShortLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ShortSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ShortSortedMaps.synchronize((Reference2ShortSortedMap) decoded)));
        register(Reference2ShortMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ShortMaps.unmodifiable((Reference2ShortMap) decoded)));
        register(Reference2ShortMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ShortMaps.unmodifiable((Reference2ShortMap) decoded)));
        register(Reference2ShortMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ShortMaps.unmodifiable((Reference2ShortMap) decoded)));
        register(Reference2ShortMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ShortOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ShortMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ShortMaps.synchronize((Reference2ShortMap) decoded)));
        register(Reference2CharMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2CharMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2CharOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2CharOpenHashMap() : ((Reference2CharOpenHashMap) current).clone()));
        register(Reference2CharArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Reference2CharArrayMap() : ((Reference2CharArrayMap) current).clone()));
        register(Reference2CharLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2CharLinkedOpenHashMap() : ((Reference2CharLinkedOpenHashMap) current).clone()));
        register(Reference2CharOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Reference2CharOpenCustomHashMap) current).clone()));
        register(Reference2CharSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2CharLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2CharSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2CharSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2CharLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2CharSortedMaps.unmodifiable((Reference2CharSortedMap) decoded)));
        register(Reference2CharSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2CharLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2CharSortedMaps.unmodifiable((Reference2CharSortedMap) decoded)));
        register(Reference2CharSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2CharLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2CharSortedMaps.unmodifiable((Reference2CharSortedMap) decoded)));
        register(Reference2CharSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2CharLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2CharSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2CharSortedMaps.synchronize((Reference2CharSortedMap) decoded)));
        register(Reference2CharMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2CharMaps.unmodifiable((Reference2CharMap) decoded)));
        register(Reference2CharMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2CharMaps.unmodifiable((Reference2CharMap) decoded)));
        register(Reference2CharMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2CharMaps.unmodifiable((Reference2CharMap) decoded)));
        register(Reference2CharMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2CharOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2CharMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2CharMaps.synchronize((Reference2CharMap) decoded)));
        register(Reference2IntMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2IntMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2IntOpenHashMap.class, (type, current) -> new Construction(
                () -> current == null ? new Reference2IntOpenHashMap() : ((Reference2IntOpenHashMap) current).clone()));
        register(Reference2IntArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Reference2IntArrayMap() : ((Reference2IntArrayMap) current).clone()));
        register(Reference2IntLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2IntLinkedOpenHashMap() : ((Reference2IntLinkedOpenHashMap) current).clone()));
        register(Reference2IntOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Reference2IntOpenCustomHashMap) current).clone()));
        register(Reference2IntSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2IntLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2IntSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2IntSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2IntLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2IntSortedMaps.unmodifiable((Reference2IntSortedMap) decoded)));
        register(Reference2IntSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2IntLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2IntSortedMaps.unmodifiable((Reference2IntSortedMap) decoded)));
        register(Reference2IntSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2IntLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2IntSortedMaps.unmodifiable((Reference2IntSortedMap) decoded)));
        register(Reference2IntSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2IntLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2IntSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2IntSortedMaps.synchronize((Reference2IntSortedMap) decoded)));
        register(Reference2IntMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2IntMaps.unmodifiable((Reference2IntMap) decoded)));
        register(Reference2IntMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2IntMaps.unmodifiable((Reference2IntMap) decoded)));
        register(Reference2IntMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2IntMaps.unmodifiable((Reference2IntMap) decoded)));
        register(Reference2IntMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2IntOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2IntMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2IntMaps.synchronize((Reference2IntMap) decoded)));
        register(Reference2LongMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2LongMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2LongOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2LongOpenHashMap() : ((Reference2LongOpenHashMap) current).clone()));
        register(Reference2LongArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Reference2LongArrayMap() : ((Reference2LongArrayMap) current).clone()));
        register(Reference2LongLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2LongLinkedOpenHashMap() : ((Reference2LongLinkedOpenHashMap) current).clone()));
        register(Reference2LongOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Reference2LongOpenCustomHashMap) current).clone()));
        register(Reference2LongSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2LongLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2LongSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2LongSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2LongLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2LongSortedMaps.unmodifiable((Reference2LongSortedMap) decoded)));
        register(Reference2LongSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2LongLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2LongSortedMaps.unmodifiable((Reference2LongSortedMap) decoded)));
        register(Reference2LongSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2LongLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2LongSortedMaps.unmodifiable((Reference2LongSortedMap) decoded)));
        register(Reference2LongSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2LongLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2LongSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2LongSortedMaps.synchronize((Reference2LongSortedMap) decoded)));
        register(Reference2LongMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2LongMaps.unmodifiable((Reference2LongMap) decoded)));
        register(Reference2LongMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2LongMaps.unmodifiable((Reference2LongMap) decoded)));
        register(Reference2LongMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2LongMaps.unmodifiable((Reference2LongMap) decoded)));
        register(Reference2LongMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2LongOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2LongMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2LongMaps.synchronize((Reference2LongMap) decoded)));
        register(Reference2FloatMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2FloatMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2FloatOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2FloatOpenHashMap() : ((Reference2FloatOpenHashMap) current).clone()));
        register(Reference2FloatArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Reference2FloatArrayMap() : ((Reference2FloatArrayMap) current).clone()));
        register(Reference2FloatLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2FloatLinkedOpenHashMap() : ((Reference2FloatLinkedOpenHashMap) current).clone()));
        register(Reference2FloatOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Reference2FloatOpenCustomHashMap) current).clone()));
        register(Reference2FloatSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2FloatLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2FloatSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2FloatSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2FloatLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2FloatSortedMaps.unmodifiable((Reference2FloatSortedMap) decoded)));
        register(Reference2FloatSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2FloatLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2FloatSortedMaps.unmodifiable((Reference2FloatSortedMap) decoded)));
        register(Reference2FloatSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2FloatLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2FloatSortedMaps.unmodifiable((Reference2FloatSortedMap) decoded)));
        register(Reference2FloatSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2FloatLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2FloatSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2FloatSortedMaps.synchronize((Reference2FloatSortedMap) decoded)));
        register(Reference2FloatMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2FloatMaps.unmodifiable((Reference2FloatMap) decoded)));
        register(Reference2FloatMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2FloatMaps.unmodifiable((Reference2FloatMap) decoded)));
        register(Reference2FloatMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2FloatMaps.unmodifiable((Reference2FloatMap) decoded)));
        register(Reference2FloatMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2FloatOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2FloatMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2FloatMaps.synchronize((Reference2FloatMap) decoded)));
        register(Reference2DoubleMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2DoubleMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2DoubleOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2DoubleOpenHashMap() : ((Reference2DoubleOpenHashMap) current).clone()));
        register(Reference2DoubleArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Reference2DoubleArrayMap() : ((Reference2DoubleArrayMap) current).clone()));
        register(Reference2DoubleLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2DoubleLinkedOpenHashMap() : ((Reference2DoubleLinkedOpenHashMap) current).clone()));
        register(Reference2DoubleOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Reference2DoubleOpenCustomHashMap) current).clone()));
        register(Reference2DoubleSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2DoubleLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2DoubleSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2DoubleLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2DoubleSortedMaps.unmodifiable((Reference2DoubleSortedMap) decoded)));
        register(Reference2DoubleSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2DoubleLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2DoubleSortedMaps.unmodifiable((Reference2DoubleSortedMap) decoded)));
        register(Reference2DoubleSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2DoubleLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2DoubleSortedMaps.unmodifiable((Reference2DoubleSortedMap) decoded)));
        register(Reference2DoubleSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2DoubleLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2DoubleSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2DoubleSortedMaps.synchronize((Reference2DoubleSortedMap) decoded)));
        register(Reference2DoubleMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2DoubleMaps.unmodifiable((Reference2DoubleMap) decoded)));
        register(Reference2DoubleMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2DoubleMaps.unmodifiable((Reference2DoubleMap) decoded)));
        register(Reference2DoubleMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2DoubleMaps.unmodifiable((Reference2DoubleMap) decoded)));
        register(Reference2DoubleMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2DoubleOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2DoubleMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2DoubleMaps.synchronize((Reference2DoubleMap) decoded)));
        register(Reference2ObjectMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ObjectMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2ObjectOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2ObjectOpenHashMap() : ((Reference2ObjectOpenHashMap) current).clone()));
        register(Reference2ObjectArrayMap.class, (type, current) -> new Construction(
                () -> current == null ? new Reference2ObjectArrayMap() : ((Reference2ObjectArrayMap) current).clone()));
        register(Reference2ObjectLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2ObjectLinkedOpenHashMap() : ((Reference2ObjectLinkedOpenHashMap) current).clone()));
        register(Reference2ObjectOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Reference2ObjectOpenCustomHashMap) current).clone()));
        register(Reference2ObjectSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ObjectLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2ObjectSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ObjectLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ObjectSortedMaps.unmodifiable((Reference2ObjectSortedMap) decoded)));
        register(Reference2ObjectSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ObjectLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ObjectSortedMaps.unmodifiable((Reference2ObjectSortedMap) decoded)));
        register(Reference2ObjectSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ObjectLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ObjectSortedMaps.unmodifiable((Reference2ObjectSortedMap) decoded)));
        register(Reference2ObjectSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ObjectLinkedOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ObjectSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ObjectSortedMaps.synchronize((Reference2ObjectSortedMap) decoded)));
        register(Reference2ObjectMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ObjectMaps.unmodifiable((Reference2ObjectMap) decoded)));
        register(Reference2ObjectMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ObjectMaps.unmodifiable((Reference2ObjectMap) decoded)));
        register(Reference2ObjectMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ObjectMaps.unmodifiable((Reference2ObjectMap) decoded)));
        register(Reference2ObjectMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ObjectOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ObjectMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ObjectMaps.synchronize((Reference2ObjectMap) decoded)));
        register(Reference2ReferenceMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ReferenceMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2ReferenceOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2ReferenceOpenHashMap() : ((Reference2ReferenceOpenHashMap) current).clone()));
        register(Reference2ReferenceArrayMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2ReferenceArrayMap() : ((Reference2ReferenceArrayMap) current).clone()));
        register(Reference2ReferenceLinkedOpenHashMap.class, (type, current) -> new Construction(() -> current == null ?
                new Reference2ReferenceLinkedOpenHashMap() : ((Reference2ReferenceLinkedOpenHashMap) current).clone()));
        register(Reference2ReferenceOpenCustomHashMap.class,
                (type, current) -> new Construction(() -> ((Reference2ReferenceOpenCustomHashMap) current).clone()));
        register(Reference2ReferenceSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ReferenceLinkedOpenHashMap();
            if (current != null)
                result.defaultReturnValue(((Reference2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }));
        register(Reference2ReferenceSortedMaps.UnmodifiableSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ReferenceLinkedOpenHashMap();
            if (current != null)
                result.defaultReturnValue(((Reference2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ReferenceSortedMaps.unmodifiable((Reference2ReferenceSortedMap) decoded)));
        register(Reference2ReferenceSortedMaps.EmptySortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ReferenceLinkedOpenHashMap();
            if (current != null)
                result.defaultReturnValue(((Reference2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ReferenceSortedMaps.unmodifiable((Reference2ReferenceSortedMap) decoded)));
        register(Reference2ReferenceSortedMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ReferenceLinkedOpenHashMap();
            if (current != null)
                result.defaultReturnValue(((Reference2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ReferenceSortedMaps.unmodifiable((Reference2ReferenceSortedMap) decoded)));
        register(Reference2ReferenceSortedMaps.SynchronizedSortedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ReferenceLinkedOpenHashMap();
            if (current != null)
                result.defaultReturnValue(((Reference2ReferenceSortedMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ReferenceSortedMaps.synchronize((Reference2ReferenceSortedMap) decoded)));
        register(Reference2ReferenceMaps.UnmodifiableMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ReferenceMaps.unmodifiable((Reference2ReferenceMap) decoded)));
        register(Reference2ReferenceMaps.EmptyMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ReferenceMaps.unmodifiable((Reference2ReferenceMap) decoded)));
        register(Reference2ReferenceMaps.Singleton.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ReferenceMaps.unmodifiable((Reference2ReferenceMap) decoded)));
        register(Reference2ReferenceMaps.SynchronizedMap.class, (type, current) -> new Construction(() -> {
            var result = new Reference2ReferenceOpenHashMap();
            if (current != null) result.defaultReturnValue(((Reference2ReferenceMap) current).defaultReturnValue());
            return result;
        }, decoded -> Reference2ReferenceMaps.synchronize((Reference2ReferenceMap) decoded)));
        register(ReferenceCollection.class, (type, current) -> new Construction(() -> new ReferenceArrayList()));
        register(ReferenceList.class, (type, current) -> new Construction(() -> new ReferenceArrayList()));
        register(ReferenceSet.class, (type, current) -> new Construction(() -> new ReferenceOpenHashSet()));
        register(ReferenceSortedSet.class, (type, current) -> new Construction(() -> new ReferenceLinkedOpenHashSet()));
        register(ReferenceBigList.class, (type, current) -> new Construction(() -> new ReferenceBigArrayBigList()));
        register(ReferenceArrayList.class, (type, current) -> new Construction(
                () -> current == null ? new ReferenceArrayList() : ((ReferenceArrayList) current).clone()));
        register(ReferenceArraySet.class, (type, current) -> new Construction(
                () -> current == null ? new ReferenceArraySet() : ((ReferenceArraySet) current).clone()));
        register(ReferenceOpenHashSet.class, (type, current) -> new Construction(
                () -> current == null ? new ReferenceOpenHashSet() : ((ReferenceOpenHashSet) current).clone()));
        register(ReferenceLinkedOpenHashSet.class, (type, current) -> new Construction(() -> current == null ?
                new ReferenceLinkedOpenHashSet() : ((ReferenceLinkedOpenHashSet) current).clone()));
        register(ReferenceBigArrayBigList.class, (type, current) -> new Construction(
                () -> current == null ? new ReferenceBigArrayBigList() : ((ReferenceBigArrayBigList) current).clone()));
        register(ReferenceCollections.unmodifiable(new ReferenceArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ReferenceArrayList(),
                        decoded -> ReferenceCollections.unmodifiable((ReferenceCollection) decoded)));
        register(ReferenceCollections.unmodifiable(new ReferenceArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ReferenceArrayList(),
                        decoded -> ReferenceCollections.unmodifiable((ReferenceCollection) decoded)));
        register(ReferenceCollections.synchronize(new ReferenceArrayList()).getClass(),
                (type, current) -> new Construction(() -> new ReferenceArrayList(),
                        decoded -> ReferenceCollections.synchronize((ReferenceCollection) decoded)));
        register(ReferenceLists.UnmodifiableList.class,
                (type, current) -> new Construction(() -> new ReferenceArrayList(),
                        decoded -> ReferenceLists.unmodifiable((ReferenceList) decoded)));
        register(ReferenceLists.EmptyList.class, (type, current) -> new Construction(() -> new ReferenceArrayList(),
                decoded -> ReferenceLists.unmodifiable((ReferenceList) decoded)));
        register(ReferenceLists.Singleton.class, (type, current) -> new Construction(() -> new ReferenceArrayList(),
                decoded -> ReferenceLists.unmodifiable((ReferenceList) decoded)));
        register(ReferenceLists.SynchronizedList.class,
                (type, current) -> new Construction(() -> new ReferenceArrayList(),
                        decoded -> ReferenceLists.synchronize((ReferenceList) decoded)));
        register(ReferenceLists.UnmodifiableRandomAccessList.class,
                (type, current) -> new Construction(() -> new ReferenceArrayList(),
                        decoded -> ReferenceLists.unmodifiable((ReferenceList) decoded)));
        register(ReferenceLists.SynchronizedRandomAccessList.class,
                (type, current) -> new Construction(() -> new ReferenceArrayList(),
                        decoded -> ReferenceLists.synchronize((ReferenceList) decoded)));
        register(ReferenceSets.UnmodifiableSet.class,
                (type, current) -> new Construction(() -> new ReferenceOpenHashSet(),
                        decoded -> ReferenceSets.unmodifiable((ReferenceSet) decoded)));
        register(ReferenceSets.EmptySet.class, (type, current) -> new Construction(() -> new ReferenceOpenHashSet(),
                decoded -> ReferenceSets.unmodifiable((ReferenceSet) decoded)));
        register(ReferenceSets.Singleton.class, (type, current) -> new Construction(() -> new ReferenceOpenHashSet(),
                decoded -> ReferenceSets.unmodifiable((ReferenceSet) decoded)));
        register(ReferenceSets.SynchronizedSet.class,
                (type, current) -> new Construction(() -> new ReferenceOpenHashSet(),
                        decoded -> ReferenceSets.synchronize((ReferenceSet) decoded)));
        register(ReferenceSortedSets.UnmodifiableSortedSet.class,
                (type, current) -> new Construction(() -> new ReferenceLinkedOpenHashSet(),
                        decoded -> ReferenceSortedSets.unmodifiable((ReferenceSortedSet) decoded)));
        register(ReferenceSortedSets.EmptySet.class,
                (type, current) -> new Construction(() -> new ReferenceLinkedOpenHashSet(),
                        decoded -> ReferenceSortedSets.unmodifiable((ReferenceSortedSet) decoded)));
        register(ReferenceSortedSets.Singleton.class,
                (type, current) -> new Construction(() -> new ReferenceLinkedOpenHashSet(),
                        decoded -> ReferenceSortedSets.unmodifiable((ReferenceSortedSet) decoded)));
        register(ReferenceSortedSets.SynchronizedSortedSet.class,
                (type, current) -> new Construction(() -> new ReferenceLinkedOpenHashSet(),
                        decoded -> ReferenceSortedSets.synchronize((ReferenceSortedSet) decoded)));
        register(ReferenceBigLists.UnmodifiableBigList.class,
                (type, current) -> new Construction(() -> new ReferenceBigArrayBigList(),
                        decoded -> ReferenceBigLists.unmodifiable((ReferenceBigList) decoded)));
        register(ReferenceBigLists.EmptyBigList.class,
                (type, current) -> new Construction(() -> new ReferenceBigArrayBigList(),
                        decoded -> ReferenceBigLists.unmodifiable((ReferenceBigList) decoded)));
        register(ReferenceBigLists.Singleton.class,
                (type, current) -> new Construction(() -> new ReferenceBigArrayBigList(),
                        decoded -> ReferenceBigLists.unmodifiable((ReferenceBigList) decoded)));
        register(ReferenceBigLists.SynchronizedBigList.class,
                (type, current) -> new Construction(() -> new ReferenceBigArrayBigList(),
                        decoded -> ReferenceBigLists.synchronize((ReferenceBigList) decoded)));
        register(ReferenceImmutableList.class, (type, current) -> new Construction(() -> new ReferenceArrayList(),
                decoded -> new ReferenceImmutableList((Collection) decoded)));
        register(new ReferenceImmutableList(new Object[] { null, null }).subList(0, 1).getClass(),
                (type, current) -> new Construction(() -> new ReferenceArrayList(),
                        decoded -> new ReferenceImmutableList((Collection) decoded)));
    }
}
