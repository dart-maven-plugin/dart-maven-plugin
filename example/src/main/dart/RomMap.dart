import "package:log4dart/log4dart.dart";

void main() {
  print("Dart geht!");
  new RomMap().init();
}

class RomMap {

  final Logger _logger;

  RomMap() : _logger = LoggerFactory.getLogger("RomMap");

  void init() {
    _logger.info("a info message");

    _logger.warnFormat("%s %s", ["a warning message", "formated using c's sprintf syntax"]);
  }
}
