import com.mipt.domainModel.Interfaces.IQuestion;

public class Question implements IQuestion {
  public int questionId;
  public Game game;

  public String questionText;
  public String answer1;
  public String answer2;
  public String answer3;
  public String answer4;
  public int rightAnswerNumber;
  public int questionNumber;

  public Question(int questionId, Game game, String questionText,
                  String answer1, String answer2, String answer3, String answer4,
                  int rightAnswerNumber, int questionNumber) {
    this.questionId = questionId;
    this.game = game;
    this.questionText = questionText;
    this.answer1 = answer1;
    this.answer2 = answer2;
    this.answer3 = answer3;
    this.answer4 = answer4;
    this.rightAnswerNumber = rightAnswerNumber;
    this.questionNumber = questionNumber;
  }

  @Override
  public int getQuestionId() {
    return questionId;
  }

  @Override
  public void setQuestionId(int questionId) {
    this.questionId = questionId;
  }

  @Override
  public Game getGame() {
    return game;
  }

  @Override
  public void setGame(Game game) {
    this.game = game;
  }

  @Override
  public String getQuestionText() {
    return questionText;
  }

  @Override
  public void setQuestionText(String questionText) {
    this.questionText = questionText;
  }

  @Override
  public String getAnswer1() {
    return answer1;
  }

  @Override
  public void setAnswer1(String answer1) {
    this.answer1 = answer1;
  }

  @Override
  public String getAnswer2() {
    return answer2;
  }

  @Override
  public void setAnswer2(String answer2) {
    this.answer2 = answer2;
  }

  @Override
  public String getAnswer3() {
    return answer3;
  }

  @Override
  public void setAnswer3(String answer3) {
    this.answer3 = answer3;
  }

  @Override
  public String getAnswer4() {
    return answer4;
  }

  @Override
  public void setAnswer4(String answer4) {
    this.answer4 = answer4;
  }

  @Override
  public int getRightAnswerNumber() {
    return rightAnswerNumber;
  }

  @Override
  public void setRightAnswerNumber(int rightAnswerNumber) {
    this.rightAnswerNumber = rightAnswerNumber;
  }

  @Override
  public int getQuestionNumber() {
    return questionNumber;
  }

  @Override
  public void setQuestionNumber(int questionNumber) {
    this.questionNumber = questionNumber;
  }
}
