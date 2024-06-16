package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.util.ArrayList;
import java.util.List;

class ReferencePropertyGetter extends AbstractPropertyGetter {

    private final ImmutableProp targetIdProp;

    ReferencePropertyGetter(ImmutableProp prop, ValueGetter valueGetter) {
        super(prop, valueGetter);
        this.targetIdProp = prop.getTargetType().getIdProp();
    }

    @Override
    public Object get(Object value) {
        ImmutableSpi spi = (ImmutableSpi) value;
        ImmutableSpi reference = (ImmutableSpi) spi.__get(prop.getId());
        if (reference == null) {
            return null;
        }
        return valueGetter.get(reference.__get(targetIdProp.getId()));
    }

    @Override
    String toStringPrefix() {
        return prop.getName() + '.' + targetIdProp.getName();
    }

    static List<PropertyGetter> getters(ImmutableProp prop, List<ValueGetter> valueGetters) {
        List<PropertyGetter> propertyGetters = new ArrayList<>(valueGetters.size());
        for (ValueGetter valueGetter : valueGetters) {
            propertyGetters.add(new ReferencePropertyGetter(prop, valueGetter));
        }
        return propertyGetters;
    }
}
