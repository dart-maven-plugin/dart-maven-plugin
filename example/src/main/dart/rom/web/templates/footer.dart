import 'dart:core';
import 'dart:html';

class footer {

//  final RegExp rootURLEnding = const RegExp(@'(^file:\/\/.*\/rom-webapp-.*\.war)\/[A-Za-z0-9\/\-]*\/\w*\.html$');

  void load() {
    String footerPath = '${rootURLEnding.firstMatch(window.location.href).group(1)}/templates/footer.html';
    HttpRequest request = new HttpRequest();
    request.open("GET", footerPath, true);
    request.on.load.add((event) {
      query('.footer_container').innerHTML = request.responseText ;
    });
    request.send(null);
  }

}

main() {
  var footer = new footer();
  footer.load();
}
