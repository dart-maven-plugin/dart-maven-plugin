part of map;

class RomCashColumn extends RomColumn {

  RomCashColumn(var cashColumnGroupTag, RomMap map) : super(cashColumnGroupTag, map);

  bool isVisible() {
    var volume = getVolume();
    return volume != null && volume != 0;
  }

  int getVolume() {
    return int.parse(_mapItemGroupTag.attributes['volume']);
  }
}