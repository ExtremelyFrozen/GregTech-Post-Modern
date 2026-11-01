package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

class Util {

    /// Convert a `Codec<A>` to `Codec<ObjectList<A>>`, where it accepts both single A value or list of A values.
    ///
    /// For example, given `oneOrMore(Codec.STRING)`, it accepts both `"Hello World"` and `["Hello", "World"]`.
    static <A> Codec<ObjectList<A>> oneOrMore(Codec<A> codec) {
        return Codec.either(codec, codec.listOf())
                .xmap(either -> either.map(ObjectList::of, ObjectArrayList::new),
                        list -> list.size() == 1 ? Either.left(list.getFirst()) : Either.right(list));
    }
}
