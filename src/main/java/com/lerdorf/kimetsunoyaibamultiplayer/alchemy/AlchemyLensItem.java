package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

public class AlchemyLensItem extends AlchemyItem {
    public AlchemyLensItem(Properties properties, int tintColor) {
        super(properties.durability(64), false, tintColor);
    }
}
