package inter;
import lexer.*;
import symbols.*;

public class Increment extends Stmt {
	public Id id;
	public Token op;
	
	public Increment(Id i, Token p) {
		id = i;
		op = p;
		if( check(id.type) == null) error("type error");
	}
	
	public Type check(Type p1) {
		if(Type.numeric(p1)) return p1;
		else return null;
	}

	public void gen(int b, int a) {
		emit(op.toString() + " " + id.toString());
	}
}
