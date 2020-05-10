package org.apache.commons.collections4.bloomfilter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.hasher.Hasher;

/**
 * A marker interface for a Bloom filter that is immutable, any attempt to
 * change it throws an UnsupportedOperationException. </p><p> This filter
 * implements hashCode() and equals() and is suitable for use in Hash based
 * collections. </p>
 */
public interface FrozenBloomFilter extends BloomFilter {

    /**
     * Creates a FrozenBloomFilter from any instance of a bloom filter using
     * a dynamic proxy.  if the argument is already a FrozenBloomFilter return it.
     * @param bloomFilter
     * @return
     */
    public static FrozenBloomFilter makeInstance(BloomFilter bloomFilter) {
        if (bloomFilter instanceof FrozenBloomFilter) {
            return (FrozenBloomFilter) bloomFilter;
        }
        Class<?> classInterfaces[] = bloomFilter.getClass().getInterfaces();
        Class<?> interfaces[] = new Class<?>[classInterfaces.length + 1];
        System.arraycopy(classInterfaces, 0, interfaces, 1, classInterfaces.length);
        interfaces[0] = FrozenBloomFilter.class;
        return (FrozenBloomFilter) Proxy.newProxyInstance(FrozenBloomFilter.class.getClassLoader(), interfaces,
                new FrozenBloomFilterInvocationHandler(bloomFilter));

    }

    public static class FrozenBloomFilterInvocationHandler implements InvocationHandler {

        private static List<Method> rejectMethods;
        private BloomFilter wrapped;

        static {
            try {

                rejectMethods = Arrays.asList(BloomFilter.class.getMethod("merge", BloomFilter.class),
                        BloomFilter.class.getMethod("merge", Hasher.class),
                        CountingBloomFilter.class.getMethod("remove", BloomFilter.class),
                        CountingBloomFilter.class.getMethod("remove", Hasher.class),
                        CountingBloomFilter.class.getMethod("add", CountingBloomFilter.class),
                        CountingBloomFilter.class.getMethod("subtract", CountingBloomFilter.class));
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }

        public FrozenBloomFilterInvocationHandler(BloomFilter bloomFilter) {
            wrapped = bloomFilter;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (rejectMethods.contains(method)) {
                throw new UnsupportedOperationException(String.format("%s not supported", method.getName()));
            }
            if (method.equals(Object.class.getMethod("equals", Object.class))) {
                return doEquals(args[0]);
            }
            if (method.equals(Object.class.getMethod("hashCode"))) {
                return doHash();
            }
            return method.invoke(wrapped, args);
        }

        private boolean doEquals(Object other) {
            if (other instanceof FrozenBloomFilter) {
                return Arrays.equals(wrapped.getBits(), ((FrozenBloomFilter) other).getBits());
            }
            return false;
        }

        private int doHash() {
            return Arrays.hashCode(wrapped.getBits());
        }

    }
}
