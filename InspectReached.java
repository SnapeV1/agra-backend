public class InspectReached {
  public static void main(String[] args) {
    for (var m : de.flapdoodle.reverse.TransitionWalker.ReachedState.class.getDeclaredMethods()) {
      System.out.println(m.toString());
    }
  }
}
