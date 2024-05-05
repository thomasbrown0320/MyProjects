package prog04;

import java.util.Stack;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import prog02.UserInterface;
import prog02.GUI;
import prog02.ConsoleUI;

public class Calculator {
  static final String OPERATORS = "()+-*/u^";
  static final int[] PRECEDENCE = { -1, -1, 1, 1, 2, 2, 3, 4 };
  Stack<Character> operatorStack = new Stack<Character>();
  Stack<Double> numberStack = new Stack<Double>();
  UserInterface ui = new GUI("Calculator");

  boolean pushedOp = false;

  Calculator (UserInterface ui) { this.ui = ui; }

  void emptyStacks () {
    while (!numberStack.empty())
      numberStack.pop();
    while (!operatorStack.empty())
      operatorStack.pop();
  }

  String numberStackToString () {
    String s = "numberStack: ";
    Stack<Double> helperStack = new Stack<Double>();
    // EXERCISE
    // Put every element of numberStack into helperStack
    // You will need to use a loop.  What kind?
    // What condition? When can you stop moving elements out of numberStack?
    // What method do you use to take an element out of numberStack?
    // What method do you use to put that element into helperStack?
    while(!numberStack.empty()) {
      helperStack.push(numberStack.pop());
    }

    // Now put everything back, but also add each one to s:
    // s = s + " " + number;
    while(!helperStack.empty()) {
      double number = helperStack.pop();
      numberStack.push(number);
      s = s + " " + number;
    }

    return s;
  }

  String operatorStackToString () {
    String s = "operatorStack: ";
    // EXERCISE
    Stack<Character> helperStack = new Stack<Character>();
    while(!operatorStack.empty()) {
      helperStack.push(operatorStack.pop());
    }
    while(!helperStack.empty()) {
      char op = helperStack.pop();
      operatorStack.push(op);
      s = s + " " + op;
    }
    return s;
  }

  int precedence(char op) {
    for(int i = 0; i < OPERATORS.length(); i++)
      if(OPERATORS.charAt(i)==op)
        return i;
    return -1;
  }

  void displayStacks () {
    ui.sendMessage(numberStackToString() + "\n" +
                   operatorStackToString());
  }

  void doNumber (double x) {
    numberStack.push(x);
    pushedOp = false;
    displayStacks();
  }

  void doOperator (char op) {
    if(op=='-' && ((operatorStack.empty() && numberStack.empty()) || pushedOp))
      op = 'u';
    processOperator(op);
    pushedOp = true;
    displayStacks();
  }

  double doEquals () {
    while (!operatorStack.empty())
      evaluateTopOperator();

    return numberStack.pop();
  }
    
  double evaluateOperator (double a, char op, double b) {
    /*
    Finish evaluateOperator by adding cases for -,*,/, and ^.  Use
      Math.pow for ^.
     */
    switch (op) {
    case '+':
      return a + b;
    case '-':
      return a - b;
    case '*':
      return a*b;
    case '/':
      return a/b;
    case '^':
      return Math.pow(a,b);

      // EXERCISE
    }
    System.out.println("Unknown operator " + op);
    return 0;
  }

  void evaluateTopOperator () {
    /*
    Implement evaluateTopOperator.  It should pop an operator off
      the operator stack and two number off the number stack.  Then
      it should call evaluateOperator and push the result on the
      number stack.  Call displayStacks() before you return.
     */
    char op = operatorStack.pop();
    if(op=='u') {
      double num = numberStack.pop();
      num *= -1;
      numberStack.push(num);
    } else {
      double a = numberStack.pop();
      double b = numberStack.pop();
      double result = evaluateOperator(b, op, a);
      numberStack.push(result);
    }
    // EXERCISE
    displayStacks();
  }

  void processOperator (char op) {
    if(op=='u' || op=='(') {
      operatorStack.push(op);
    } else if(op==')') {
      while (!operatorStack.empty() && !operatorStack.peek().equals('('))
        evaluateTopOperator();
      operatorStack.pop();
    } else {
    /*
    While the top element (if there is one) of the operator stack has
  precedence >= than the precedence of op, evaluate it (call evaluateTopOperator()), then push op on the stack.
  */
      while (!operatorStack.empty() && precedence(operatorStack.peek()) >= precedence(op)) {
        evaluateTopOperator();
      }
      operatorStack.push(op);
    }
  }
  
  static boolean checkTokens (UserInterface ui, Object[] tokens) {
      for (Object token : tokens)
        if (token instanceof Character &&
            OPERATORS.indexOf((Character) token) == -1) {
          ui.sendMessage(token + " is not a valid operator.");
          return false;
        }
      return true;
  }

  static void processExpressions (UserInterface ui, Calculator calculator) {
    while (true) {
      String line = ui.getInfo("Enter arithmetic expression or cancel.");
      if (line == null)
        return;
      Object[] tokens = Tokenizer.tokenize(line);
      if (!checkTokens(ui, tokens))
        continue;
      try {
        for (Object token : tokens)
          if (token instanceof Double)
            calculator.doNumber((Double) token);
          else          
            calculator.doOperator((Character) token);
        double result = calculator.doEquals();
        ui.sendMessage(line + " = " + result);
      } catch (Exception e) {
        ui.sendMessage("Bad expression.");
        calculator.emptyStacks();
      }
    }
  }

  public static void main (String[] args) {
    UserInterface ui = new ConsoleUI();
    Calculator calculator = new Calculator(ui);
    processExpressions(ui, calculator);
  }
}
