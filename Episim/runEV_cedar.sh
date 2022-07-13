cpus=40
time=24:00:00
mem='150G'

folder=/home/renanong/scratch/evMontreal
job_config=$folder/config.xml
output=urban_ev_22_02_2022



##############################################
echo "#!/bin/bash
#SBATCH --nodes=1
#SBATCH --cpus-per-task=$cpus
#SBATCH --ntasks=1
#SBATCH --time=$time
#SBATCH --mem=$mem
#SBATCH --job-name $output
#SBATCH --account=def-fciari
#SBATCH --mail-user=yonsorena.nong@polymtl.ca
#SBATCH --mail-type=ALL

echo "Current working directory: `pwd`"
echo "Starting run at: `date`"

#Load environment
module load StdEnv/2020

# Load Java
module load java/14.0.2

# Load script
java -Xmx$mem -cp urbanEV.jar EVPricing.RunEvExample $job_config

" > job_${output}.sh
###############################################

chmod +x job_${output}.sh

#srun -n 1 -c $cpus --mem=$mem --time=$time ./job_${output}.sh

sbatch ./job_${output}.sh

# ---------------------------------------------------------------------
echo "Job finished with exit code $? at: `date`"


