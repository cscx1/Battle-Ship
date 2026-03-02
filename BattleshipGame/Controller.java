/**
in MVC, the controller connects the view (clicks, buttons) to the model
(game logic). in this skeleton we keep it minimal: the view talks to the
model directly when you click the grid or the play button. you could later
move that logic here so all user input goes through the controller.
 */
public class Controller {
    private Model model;
    private View view;

    public Controller(Model model, View view) {
        this.model = model;
        this.view = view;
    }
}
