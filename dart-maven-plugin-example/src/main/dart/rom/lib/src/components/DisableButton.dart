part of components;

class DisableButton {

  LIElement liButton;
  List<ImageElement> images;

  DisableButton(final String queryValue) {
    liButton = query(queryValue);
    images = liButton.queryAll("img");
  }

  void activate() {
    images[0].hidden = true;
    images[1].hidden = false;
  }

  void deactivate() {
    images[0].hidden = false;
    images[1].hidden = true;
  }
}