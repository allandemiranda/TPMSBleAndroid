package lu.allandemiranda.tpms.controller;

import android.content.Context;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lu.allandemiranda.tpms.model.ManofactureData;
import lu.allandemiranda.tpms.model.Tpms;
import lu.allandemiranda.tpms.service.BleScanner;
import lu.allandemiranda.tpms.util.TpmsDecoder;

public final class TpmsScanner {

    private final BleScanner bleScanner;

    public TpmsScanner(Context context) {
        this.bleScanner = new BleScanner(context);
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public Tpms[] getTpms(String macFront, String macRear) {
        Set<String> wanted = Set.of(macFront, macRear);

        List<ManofactureData> firsts = bleScanner.getManufacturerData(2000).reversed().stream().filter(md -> wanted.contains(md.mac())).filter(distinctByKey(ManofactureData::mac)).limit(2).toList();

        Map<String, Tpms> map = firsts.parallelStream().map(md -> {
            TpmsDecoder.TPMS d = TpmsDecoder.decode(md.data());
            return (d == null) ? new Tpms(md.mac(), 101, 0, md.rssi()) : new Tpms(md.mac(), d.pressure(), d.temperature(), md.rssi());
        }).collect(Collectors.toConcurrentMap(Tpms::mac, Function.identity()));

        return new Tpms[]{map.get(macFront), map.get(macRear)};
    }
}
