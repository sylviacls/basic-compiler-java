package inter;
import symbols.*;

public class For extends Stmt{
	Expr expr;
	Stmt stmt1;
	Stmt stmt2;
	Stmt stmt3;
	
	public For() {
		expr = null;
		stmt1 = null;
		stmt2 = null;
		stmt3 = null;
	}
	
	public void init (Stmt s1, Expr x, Stmt s2, Stmt s3) {
		expr = x;
		stmt1 = s1;
		stmt2 = s2;
		stmt3 = s3;
		if(expr.type != Type.Bool) expr.error("boolean required in white");
	}
	
	public void gen(int b , int a) {
		stmt1.gen(b, a);
		int label1 = newlabel();
		emitlabel(label1);
		after = a;
		expr.jumping(0, a);
		int label2 = newlabel();
		emitlabel(label2);
		stmt3.gen(label2, b);
		stmt2.gen(b, a);
		emit("goto L" + label1);
	}
}
