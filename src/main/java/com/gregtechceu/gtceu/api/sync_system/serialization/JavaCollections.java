package com.gregtechceu.gtceu.api.sync_system.serialization;

import com.gregtechceu.gtceu.api.sync_system.serialization.CollectionCodecs.Construction;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/** Common JVM collection registrations; factories copy policy as well as contents. */
@SuppressWarnings({ "rawtypes", "unchecked" }) // Generic elements are resolved by the enclosing field codec.
public final class JavaCollections {

    private static final Map<Class<?>, BiFunction<Type, Object, Construction>> FACTORIES = new Object2ObjectOpenHashMap<>();
    private static final Set<Class<?>> READ_ONLY = new ObjectOpenHashSet<>();
    private static final Class<?> ARRAY_LIST = Arrays.asList(new Object()).getClass();

    static {
        register(Map.class, (type, current) -> plan(Object2ObjectOpenHashMap::new));
        register(Collection.class, (type, current) -> plan(ObjectArrayList::new));
        register(List.class, (type, current) -> plan(ObjectArrayList::new));
        register(Set.class, (type, current) -> plan(ObjectOpenHashSet::new));
        register(SortedMap.class, (type, current) -> plan(() -> new TreeMap(comparator(current))));
        register(NavigableMap.class, (type, current) -> plan(() -> new TreeMap(comparator(current))));
        register(SortedSet.class, (type, current) -> plan(() -> new TreeSet(comparator(current))));
        register(NavigableSet.class, (type, current) -> plan(() -> new TreeSet(comparator(current))));
        register(Queue.class, (type, current) -> plan(ArrayDeque::new));
        register(Deque.class, (type, current) -> plan(ArrayDeque::new));
        register(ConcurrentMap.class, (type, current) -> plan(ConcurrentHashMap::new));
        register(ConcurrentNavigableMap.class,
                (type, current) -> plan(() -> new ConcurrentSkipListMap(comparator(current))));

        register(HashMap.class,
                (type, current) -> plan(() -> current == null ? new HashMap() : ((HashMap) current).clone()));
        register(LinkedHashMap.class,
                (type,
                 current) -> plan(() -> current == null ? new LinkedHashMap() : ((LinkedHashMap) current).clone()));
        register(IdentityHashMap.class,
                (type,
                 current) -> plan(() -> current == null ? new IdentityHashMap() : ((IdentityHashMap) current).clone()));
        register(WeakHashMap.class, (type, current) -> plan(WeakHashMap::new));
        register(TreeMap.class,
                (type, current) -> plan(() -> current == null ? new TreeMap() : ((TreeMap) current).clone()));
        register(EnumMap.class, (type, current) -> plan(
                () -> current == null ? new EnumMap(enumType(type, Map.class)) : ((EnumMap) current).clone()));
        register(EnumSet.class, (type, current) -> plan(() -> current == null ?
                EnumSet.noneOf(enumType(type, Collection.class)) : ((EnumSet) current).clone()));

        register(HashSet.class,
                (type, current) -> plan(() -> current == null ? new HashSet() : ((HashSet) current).clone()));
        register(LinkedHashSet.class,
                (type,
                 current) -> plan(() -> current == null ? new LinkedHashSet() : ((LinkedHashSet) current).clone()));
        register(TreeSet.class,
                (type, current) -> plan(() -> current == null ? new TreeSet() : ((TreeSet) current).clone()));
        register(ArrayList.class,
                (type, current) -> plan(() -> current == null ? new ArrayList() : ((ArrayList) current).clone()));
        register(LinkedList.class,
                (type, current) -> plan(() -> current == null ? new LinkedList() : ((LinkedList) current).clone()));
        register(Vector.class,
                (type, current) -> plan(() -> current == null ? new Vector() : ((Vector) current).clone()));
        register(Stack.class, (type, current) -> plan(() -> current == null ? new Stack() : ((Stack) current).clone()));
        register(ArrayDeque.class,
                (type, current) -> plan(() -> current == null ? new ArrayDeque() : ((ArrayDeque) current).clone()));
        register(PriorityQueue.class, (type, current) -> plan(
                () -> current == null ? new PriorityQueue() : new PriorityQueue((PriorityQueue) current)));
        register(ConcurrentHashMap.class, (type, current) -> plan(ConcurrentHashMap::new));
        register(ConcurrentSkipListMap.class,
                (type, current) -> plan(() -> new ConcurrentSkipListMap(comparator(current))));
        register(ConcurrentSkipListSet.class,
                (type, current) -> plan(() -> new ConcurrentSkipListSet(comparator(current))));
        register(ConcurrentLinkedQueue.class, (type, current) -> plan(ConcurrentLinkedQueue::new));
        register(ConcurrentLinkedDeque.class, (type, current) -> plan(ConcurrentLinkedDeque::new));
        register(CopyOnWriteArrayList.class, (type, current) -> plan(CopyOnWriteArrayList::new));
        register(CopyOnWriteArraySet.class, (type, current) -> plan(CopyOnWriteArraySet::new));

        readOnly(Collections.emptyMap().getClass(), Object2ObjectOpenHashMap::new,
                value -> Collections.unmodifiableMap((Map) value));
        readOnly(Collections.singletonMap(0, 0).getClass(), Object2ObjectOpenHashMap::new,
                value -> Collections.unmodifiableMap((Map) value));
        readOnly(Collections.emptyList().getClass(), ObjectArrayList::new,
                value -> Collections.unmodifiableList((List) value));
        readOnly(Collections.singletonList(0).getClass(), ObjectArrayList::new,
                value -> Collections.unmodifiableList((List) value));
        readOnly(Collections.emptySet().getClass(), ObjectOpenHashSet::new,
                value -> Collections.unmodifiableSet((Set) value));
        readOnly(Collections.singleton(0).getClass(), ObjectOpenHashSet::new,
                value -> Collections.unmodifiableSet((Set) value));
        readOnly(Map.of().getClass(), Object2ObjectOpenHashMap::new, value -> Map.copyOf((Map) value));
        readOnly(Map.of(0, 0).getClass(), Object2ObjectOpenHashMap::new, value -> Map.copyOf((Map) value));
        readOnly(List.of().getClass(), ObjectArrayList::new, value -> List.copyOf((List) value));
        readOnly(List.of(0).getClass(), ObjectArrayList::new, value -> List.copyOf((List) value));
        readOnly(List.of(0, 1).subList(0, 1).getClass(), ObjectArrayList::new,
                value -> List.copyOf((List) value));
        readOnly(Set.of().getClass(), ObjectOpenHashSet::new, value -> Set.copyOf((Set) value));
        readOnly(Set.of(0).getClass(), ObjectOpenHashSet::new, value -> Set.copyOf((Set) value));
        readOnly(Collections.unmodifiableMap(new HashMap<>()).getClass(), Object2ObjectOpenHashMap::new,
                value -> Collections.unmodifiableMap((Map) value));
        readOnly(Collections.unmodifiableCollection(new ArrayList<>()).getClass(), ObjectArrayList::new,
                value -> Collections.unmodifiableCollection((Collection) value));
        readOnly(Collections.unmodifiableList(new ArrayList<>()).getClass(), ObjectArrayList::new,
                value -> Collections.unmodifiableList((List) value));
        readOnly(Collections.unmodifiableList(new LinkedList<>()).getClass(), LinkedList::new,
                value -> Collections.unmodifiableList((List) value));
        readOnly(Collections.unmodifiableSet(new HashSet<>()).getClass(), ObjectOpenHashSet::new,
                value -> Collections.unmodifiableSet((Set) value));
        sortedMap(Collections.emptySortedMap().getClass(), false);
        sortedMap(Collections.emptyNavigableMap().getClass(), false);
        sortedMap(Collections.unmodifiableSortedMap(new TreeMap<>()).getClass(), false);
        sortedMap(Collections.unmodifiableNavigableMap(new TreeMap<>()).getClass(), false);
        sortedSet(Collections.emptySortedSet().getClass(), false);
        sortedSet(Collections.emptyNavigableSet().getClass(), false);
        sortedSet(Collections.unmodifiableSortedSet(new TreeSet<>()).getClass(), false);
        sortedSet(Collections.unmodifiableNavigableSet(new TreeSet<>()).getClass(), false);
        sortedMap(Collections.synchronizedSortedMap(new TreeMap<>()).getClass(), true);
        sortedMap(Collections.synchronizedNavigableMap(new TreeMap<>()).getClass(), true);
        sortedSet(Collections.synchronizedSortedSet(new TreeSet<>()).getClass(), true);
        sortedSet(Collections.synchronizedNavigableSet(new TreeSet<>()).getClass(), true);
        register(Collections.synchronizedMap(new HashMap<>()).getClass(),
                (type, current) -> new Construction(Object2ObjectOpenHashMap::new,
                        value -> Collections.synchronizedMap((Map) value)));
        register(Collections.synchronizedCollection(new ArrayList<>()).getClass(),
                (type, current) -> new Construction(ObjectArrayList::new,
                        value -> Collections.synchronizedCollection((Collection) value)));
        register(Collections.synchronizedList(new ArrayList<>()).getClass(),
                (type, current) -> new Construction(ObjectArrayList::new,
                        value -> Collections.synchronizedList((List) value)));
        register(Collections.synchronizedList(new LinkedList<>()).getClass(),
                (type, current) -> new Construction(LinkedList::new,
                        value -> Collections.synchronizedList((List) value)));
        register(Collections.synchronizedSet(new HashSet<>()).getClass(),
                (type, current) -> new Construction(ObjectOpenHashSet::new,
                        value -> Collections.synchronizedSet((Set) value)));
        register(ARRAY_LIST, (type, current) -> new Construction(ObjectArrayList::new,
                value -> Arrays.asList(((List) value).toArray())));
    }

    private JavaCollections() {}

    public static Construction construction(Type type, Object current) {
        if (current instanceof EnumSet set) return plan(set::clone);
        var factory = FACTORIES.get(current == null ? Types.raw(type) : current.getClass());
        if (factory == null) factory = FACTORIES.get(Types.raw(type));
        return factory == null ? null : factory.apply(type, current);
    }

    public static boolean isReadOnly(Object current) {
        return READ_ONLY.contains(current.getClass());
    }

    public static boolean isFixedSize(Object current) {
        return current.getClass() == ARRAY_LIST;
    }

    public static Comparator comparator(Object current) {
        return switch (current) {
            case null -> null;
            case SortedMap map -> map.comparator();
            case SortedSet set -> set.comparator();
            default -> null;
        };
    }

    private static Class enumType(Type type, Class<?> contract) {
        return Types.raw(Types.arguments(type, contract).getFirst());
    }

    private static Construction plan(Supplier<Object> create) {
        return new Construction(create::get);
    }

    private static void readOnly(Class<?> type, Supplier<Object> create, UnaryOperator<Object> finish) {
        READ_ONLY.add(type);
        register(type, (declaration, current) -> new Construction(create::get, finish::apply));
    }

    private static void sortedMap(Class<?> type, boolean synchronizedMap) {
        if (!synchronizedMap) READ_ONLY.add(type);
        register(type, (declaration, current) -> new Construction(() -> new TreeMap(comparator(current)),
                value -> synchronizedMap ? Collections.synchronizedNavigableMap((NavigableMap) value) :
                        Collections.unmodifiableNavigableMap((NavigableMap) value)));
    }

    private static void sortedSet(Class<?> type, boolean synchronizedSet) {
        if (!synchronizedSet) READ_ONLY.add(type);
        register(type, (declaration, current) -> new Construction(() -> new TreeSet(comparator(current)),
                value -> synchronizedSet ? Collections.synchronizedNavigableSet((NavigableSet) value) :
                        Collections.unmodifiableNavigableSet((NavigableSet) value)));
    }

    private static void register(Class<?> type, BiFunction<Type, Object, Construction> factory) {
        FACTORIES.put(type, factory);
    }
}
