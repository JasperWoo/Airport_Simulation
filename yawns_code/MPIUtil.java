import mpi.Datatype;
import mpi.MPI;

public class MPIUtil {
	public static void allToAll(double[] sendbuf, int sendoffset, int[] sendcount, int[] sdispls,
			Datatype sendtype, double[] recvbuf, int recvoffset, int[] recvcount, int[] rdispls, 
			Datatype recvtype) {
		MPI.COMM_WORLD.Alltoallv(sendbuf, sendoffset, sendcount, sdispls, sendtype, recvbuf,
				recvoffset, recvcount, rdispls, recvtype);
	}
}
