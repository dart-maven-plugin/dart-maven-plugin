part of map;

class RomRecommendationColumn extends RomColumn {

  RomRecommendationColumn(var recommendationColumnGroupTag, RomMap map) : super(recommendationColumnGroupTag, map);

  bool isVisible() {
    return true;
  }

}