package io.github.lightman314.lightmanscurrency.common.core.groups;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.util.TriConsumer;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;
import java.util.function.Supplier;

public class RegistryObjectBiBundle<T,L,M> {

    private final Comparator<L> sorter1;
    private final Comparator<M> sorter2;

    private boolean locked = false;
    public RegistryObjectBiBundle<T,L,M> lock() { this.locked = true; return this; }

    public RegistryObjectBiBundle(Comparator<L> sorter1, Comparator<M> sorter2) { this.sorter1 = sorter1; this.sorter2 = sorter2; }

    private final Map<L, Map<M,RegistryObject<T>>> values = new HashMap<>();

    public void put(L key1, M key2, RegistryObject<T> value) {
        if(this.locked)
        {
            LightmansCurrency.LogWarning("Attempted to put an object in the bundle after it's been locked.");
            return;
        }
        Map<M,RegistryObject<T>> childMap = this.values.getOrDefault(key1, new HashMap<>());
        if(childMap.containsKey(key2))
        {
            LightmansCurrency.LogWarning("Attempted to put a second object with key '" + key1.toString() + "," + key2.toString() + "' into the registry bundle.");
            return;
        }
        childMap.put(key2, value);
        this.values.put(key1,childMap);
    }

    public RegistryObject<T> getRegistryObject(L key1, M key2) {
        if(this.values.containsKey(key1))
        {
            Map<M,RegistryObject<T>> childMap = this.values.get(key1);
            if(childMap.containsKey(key2))
                return childMap.get(key2);
        }
        return null;
    }

    public T get(L key1, M key2) {
        RegistryObject<T> result = this.getRegistryObject(key1, key2);
        if(result != null)
            return result.get();
        return null;
    }

    public Collection<RegistryObject<T>> getAllRegistryObjects() {
        List<RegistryObject<T>> result = new ArrayList<>();
        this.values.values().forEach(childMap -> result.addAll(childMap.values()));
        return result;
    }

    public List<T> getAll() {
        List<T> values = new ArrayList<>();
        for(RegistryObject<T> value : this.getAllRegistryObjects())
            values.add(value.get());
        return values;
    }

    private List<L> getKey1Sorted() { return this.getKey1Sorted(this.sorter1); }
    private List<L> getKey1Sorted(Comparator<L> sorter) {
        List<L> keys = new ArrayList<>(this.values.keySet());
        keys.sort(sorter);
        return keys;
    }

    private List<M> getKey2Sorted(Map<M,RegistryObject<T>> map) { return this.getKey2Sorted(map, this.sorter2); }
    private List<M> getKey2Sorted(Map<M,RegistryObject<T>> map, Comparator<M> sorter) {
        List<M> keys = new ArrayList<>(map.keySet());
        keys.sort(sorter);
        return keys;
    }

    public List<T> getAllSorted() { return this.getAllSorted(this.sorter1, this.sorter2); }

    public List<T> getAllSorted(Comparator<L> sorter1, Comparator<M> sorter2)
    {
        List<L> keys1 = this.getKey1Sorted();

        List<T> result = new ArrayList<>();
        for(L key1 : keys1)
        {
            List<M> keys2 = this.getKey2Sorted(this.values.get(key1));
            for(M key2 : keys2)
                result.add(this.get(key1, key2));
        }
        return result;
    }

    public List<Supplier<T>> getSupplier() {
        List<Supplier<T>> result = new ArrayList<>();
        for(L key1 : this.values.keySet())
        {
            Map<M,RegistryObject<T>> childMap = this.values.get(key1);
            for(M key2 : childMap.keySet())
                result.add(() -> this.get(key1, key2));
        }
        return result;
    }

    public void forEach(TriConsumer<L,M,RegistryObject<T>> consumer) {
        List<L> key1 = this.getKey1Sorted();
        for(L k1 : key1)
        {
            Map<M,RegistryObject<T>> map = this.values.get(k1);
            List<M> key2 = this.getKey2Sorted(map);
            for(M k2 : key2)
                consumer.accept(k1,k2,map.get(k2));
        }
    }

}